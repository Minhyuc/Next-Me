package com.nm.personalcolorservice.Service;

import com.nm.personalcolorservice.Dto.AiPersonalColorAnalysisResultDto;
import com.nm.personalcolorservice.Dto.PageResponseDto;
import com.nm.personalcolorservice.Dto.PersonalColorChatRequestDto;
import com.nm.personalcolorservice.Dto.PersonalColorChatResponseDto;
import com.nm.personalcolorservice.Dto.PersonalColorConversationResponseDto;
import com.nm.personalcolorservice.Dto.PersonalColorMessageResponseDto;
import com.nm.personalcolorservice.Entity.PersonalColorConversation;
import com.nm.personalcolorservice.Entity.PersonalColorMessage;
import com.nm.personalcolorservice.Repository.PersonalColorConversationRepository;
import com.nm.personalcolorservice.Repository.PersonalColorMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
/** 퍼스널컬러 분석 대화의 생성, 저장, AI 호출, 판정 완료를 담당한다. */
public class PersonalColorAnalysisService {

    private static final String FIRST_QUESTION =
            "퍼스널컬러를 함께 찾아볼게요. 먼저 햇빛을 받았을 때 피부가 쉽게 붉어지는 편인가요, 아니면 비교적 잘 타는 편인가요?";

    private final PersonalColorConversationRepository conversationRepository;
    private final PersonalColorMessageRepository messageRepository;
    private final OpenAiPersonalColorAnalysisService openAiService;
    private final UserProfileClientService profileClient;

    public PersonalColorAnalysisService(
            PersonalColorConversationRepository conversationRepository,
            PersonalColorMessageRepository messageRepository,
            OpenAiPersonalColorAnalysisService openAiService,
            UserProfileClientService profileClient
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.openAiService = openAiService;
        this.profileClient = profileClient;
    }

    @Transactional
    // 대화를 명시적으로 새로 시작할 때 사용한다.
    public PersonalColorChatResponseDto start(Long userId) {
        PersonalColorConversation conversation = conversationRepository.save(
                PersonalColorConversation.builder()
                        .userId(userId)
                        .status("ACTIVE")
                        .build()
        );
        saveMessage(conversation, "ASSISTANT", FIRST_QUESTION);
        return PersonalColorChatResponseDto.builder()
                .conversationId(conversation.getId())
                .reply(FIRST_QUESTION)
                .completed(false)
                .build();
    }

    @Transactional
    // 사용자 메시지와 이전 이력을 AI에 전달하고 대화를 한 단계 진행한다.
    public PersonalColorChatResponseDto chat(
            Long userId,
            String authorizationHeader,
            PersonalColorChatRequestDto request
    ) {
        PersonalColorConversation conversation = resolveConversation(userId, request.getConversationId());
        if ("COMPLETED".equals(conversation.getStatus())) {
            throw new IllegalArgumentException("이미 완료된 퍼스널컬러 대화입니다. 새 분석을 시작해주세요.");
        }

        // AI가 문맥을 이해할 수 있도록 사용자와 AI의 전체 분석 대화를 시간순으로 전달한다.
        saveMessage(conversation, "USER", request.getMessage().trim());
        List<PersonalColorMessage> messages =
                messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId());
        AiPersonalColorAnalysisResultDto result = openAiService.analyze(
                profileClient.getProfileJson(authorizationHeader), messages
        );
        saveMessage(conversation, "ASSISTANT", result.reply());
        conversation.touch();

        if (result.completed()) {
            // 판정이 끝난 경우 대화 상태와 기존 user-service 프로필을 함께 갱신한다.
            conversation.complete(result.personalColor(), result.confidence());
            profileClient.updatePersonalColor(authorizationHeader, result.personalColor());
        }
        conversationRepository.save(conversation);

        return PersonalColorChatResponseDto.builder()
                .conversationId(conversation.getId())
                .reply(result.reply())
                .completed(result.completed())
                .personalColor(result.personalColor())
                .confidence(result.confidence())
                .build();
    }

    @Transactional(readOnly = true)
    // 최신 활동 대화부터 페이지 단위로 내려준다.
    public PageResponseDto<PersonalColorConversationResponseDto> getConversations(
            Long userId,
            int page,
            int size
    ) {
        return PageResponseDto.from(
                conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(
                        userId, PageRequest.of(page, size)
                ),
                conversation -> PersonalColorConversationResponseDto.builder()
                        .conversationId(conversation.getId())
                        .status(conversation.getStatus())
                        .personalColor(conversation.getPersonalColor())
                        .confidence(conversation.getConfidence())
                        .updatedAt(conversation.getUpdatedAt())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    // 다른 사용자의 conversationId로 접근하지 못하도록 소유권을 먼저 확인한다.
    public PageResponseDto<PersonalColorMessageResponseDto> getMessages(
            Long userId,
            Long conversationId,
            int page,
            int size
    ) {
        getOwnedConversation(userId, conversationId);
        return PageResponseDto.from(
                messageRepository.findAllByConversationIdOrderByCreatedAtDesc(
                        conversationId, PageRequest.of(page, size)
                ),
                message -> PersonalColorMessageResponseDto.builder()
                        .messageId(message.getId())
                        .role(message.getRole())
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt())
                        .build()
        );
    }

    private PersonalColorConversation resolveConversation(Long userId, Long conversationId) {
        // ID가 없으면 사용자의 진행 중 대화를 재사용하고, 없을 때만 새 대화를 만든다.
        if (conversationId != null) {
            return getOwnedConversation(userId, conversationId);
        }
        return conversationRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, "ACTIVE")
                .orElseGet(() -> {
                    PersonalColorConversation created = conversationRepository.save(
                            PersonalColorConversation.builder()
                                    .userId(userId)
                                    .status("ACTIVE")
                                    .build()
                    );
                    saveMessage(created, "ASSISTANT", FIRST_QUESTION);
                    return created;
                });
    }

    private PersonalColorConversation getOwnedConversation(Long userId, Long conversationId) {
        // ID와 userId를 동시에 조건으로 사용하여 타인의 대화 조회를 차단한다.
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 퍼스널컬러 대화를 찾을 수 없습니다."));
    }

    private void saveMessage(
            PersonalColorConversation conversation,
            String role,
            String content
    ) {
        messageRepository.save(PersonalColorMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build());
    }
}
