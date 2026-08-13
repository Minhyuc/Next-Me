package com.nm.aichatservice.Service;

import com.nm.aichatservice.Dto.ChatRequestDto;
import com.nm.aichatservice.Dto.ChatResponseDto;
import com.nm.aichatservice.Dto.ConversationResponseDto;
import com.nm.aichatservice.Dto.MessageResponseDto;
import com.nm.aichatservice.Dto.OpenAiResultDto;
import com.nm.aichatservice.Entity.ChatConversation;
import com.nm.aichatservice.Entity.ChatMessage;
import com.nm.aichatservice.Entity.MessageRole;
import com.nm.aichatservice.Repository.ChatConversationRepository;
import com.nm.aichatservice.Repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 대화방 관리, 메시지 저장, 프로필 조회와 AI 호출을 하나의 대화 흐름으로 묶는다. */
@Service
public class AiChatService {

    // AI가 범위 밖으로 판정했을 때 모델 답변 대신 항상 반환할 고정 문구다.
    public static final String OUT_OF_SCOPE_MESSAGE = "범위에서 벗어난 질문입니다.\n"
            + "피부, 운동, 루틴과 관련된 내용을 질문해주세요.";

    private static final int HISTORY_LIMIT = 20;
    private static final int TITLE_MAX_LENGTH = 40;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final OpenAiClientService openAiClientService;
    private final UserProfileClientService userProfileClientService;

    public AiChatService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            OpenAiClientService openAiClientService,
            UserProfileClientService userProfileClientService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.openAiClientService = openAiClientService;
        this.userProfileClientService = userProfileClientService;
    }

    @Transactional
    /** 질문과 AI 답변을 한 트랜잭션으로 저장하고 프론트용 응답을 만든다. */
    public ChatResponseDto chat(Long userId, String authorizationHeader, ChatRequestDto dto) {
        // conversationId가 없으면 첫 질문으로 새 대화방을 만들고, 있으면 소유권까지 확인한다.
        ChatConversation conversation = getOrCreateConversation(userId, dto);

        // AI 호출 전에 사용자 질문부터 저장하여 최신 문맥에 포함한다.
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(dto.getMessage().trim())
                .outOfScope(false)
                .build();
        messageRepository.save(userMessage);

        // Repository 결과는 최신순이므로 OpenAI가 읽기 자연스러운 과거→현재 순으로 뒤집는다.
        List<ChatMessage> history = new ArrayList<>(
                messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversation.getId())
        );
        Collections.reverse(history);

        // 동일 JWT를 user-service에 전달해 현재 사용자의 프로필을 상담 문맥으로 가져온다.
        String profileContext = userProfileClientService.getProfileContext(authorizationHeader);
        OpenAiResultDto aiResult = openAiClientService.generateReply(
                history.subList(Math.max(0, history.size() - HISTORY_LIMIT), history.size()),
                profileContext
        );

        // 범위 밖이면 모델이 임의로 만든 문장 대신 서비스 정책의 고정 안내를 사용한다.
        boolean outOfScope = !aiResult.isInScope();
        String answer = outOfScope ? OUT_OF_SCOPE_MESSAGE : aiResult.getAnswer();

        // 최종적으로 사용자에게 보여준 AI 답변도 이력 DB에 저장한다.
        ChatMessage assistantMessage = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(answer)
                .outOfScope(outOfScope)
                .build());

        // 대화방 목록에서 방금 사용한 대화가 가장 먼저 보이도록 수정 시각을 갱신한다.
        conversation.touch();
        conversationRepository.save(conversation);

        return ChatResponseDto.builder()
                .conversationId(conversation.getId())
                .answer(answer)
                .outOfScope(outOfScope)
                .createdAt(assistantMessage.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    /** 현재 사용자의 대화방만 최근 사용 순서로 반환한다. */
    public List<ConversationResponseDto> getConversations(Long userId) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /** 대화방 소유권을 확인한 뒤 메시지 전체를 시간순으로 반환한다. */
    public List<MessageResponseDto> getMessages(Long userId, Long conversationId) {
        findConversation(userId, conversationId);
        return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    /** 본인 대화방만 삭제하며 엔티티 cascade 설정으로 메시지도 함께 제거한다. */
    public void deleteConversation(Long userId, Long conversationId) {
        ChatConversation conversation = findConversation(userId, conversationId);
        conversationRepository.delete(conversation);
    }

    private ChatConversation getOrCreateConversation(Long userId, ChatRequestDto dto) {
        if (dto.getConversationId() != null) {
            return findConversation(userId, dto.getConversationId());
        }

        // 새 대화방 제목은 별도 입력 없이 첫 질문 앞부분으로 자동 생성한다.
        String title = dto.getMessage().trim();
        if (title.length() > TITLE_MAX_LENGTH) {
            title = title.substring(0, TITLE_MAX_LENGTH) + "...";
        }

        return conversationRepository.save(ChatConversation.builder()
                .userId(userId)
                .title(title)
                .build());
    }

    private ChatConversation findConversation(Long userId, Long conversationId) {
        // 사용자 ID까지 조건에 포함해 URL의 ID만 바꿔 타인의 대화를 읽는 것을 막는다.
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));
    }

    private ConversationResponseDto toConversationResponse(ChatConversation conversation) {
        return ConversationResponseDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private MessageResponseDto toMessageResponse(ChatMessage message) {
        return MessageResponseDto.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .outOfScope(message.isOutOfScope())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
