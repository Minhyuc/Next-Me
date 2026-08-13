package com.nm.aichatservice.Controller;

import com.nm.aichatservice.Dto.ChatRequestDto;
import com.nm.aichatservice.Dto.ChatResponseDto;
import com.nm.aichatservice.Dto.ConversationResponseDto;
import com.nm.aichatservice.Dto.MessageResponseDto;
import com.nm.aichatservice.Service.AiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 대화 생성, 대화방 목록 조회, 메시지 이력 조회와 삭제 API를 제공한다.
 * /me 경로는 요청 본문이 아닌 JWT에서 현재 사용자를 식별한다는 의미다.
 */
@RestController
@RequestMapping("/ai-chat/me")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/messages")
    /** 새 대화를 시작하거나 conversationId에 해당하는 기존 대화를 이어간다. */
    public ResponseEntity<ChatResponseDto> chat(
            HttpServletRequest request,
            @Valid @RequestBody ChatRequestDto dto
    ) {
        return ResponseEntity.ok(aiChatService.chat(
                getUserId(request),
                request.getHeader("Authorization"),
                dto
        ));
    }

    @GetMapping("/conversations")
    /** 현재 사용자의 대화방을 최근 수정 순으로 조회한다. */
    public ResponseEntity<List<ConversationResponseDto>> getConversations(HttpServletRequest request) {
        return ResponseEntity.ok(aiChatService.getConversations(getUserId(request)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    /** 선택한 대화방의 전체 메시지를 오래된 순서부터 반환한다. */
    public ResponseEntity<List<MessageResponseDto>> getMessages(
            HttpServletRequest request,
            @PathVariable Long conversationId
    ) {
        return ResponseEntity.ok(aiChatService.getMessages(getUserId(request), conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    /** 현재 사용자 소유의 대화방과 연결된 메시지를 함께 삭제한다. */
    public ResponseEntity<Void> deleteConversation(
            HttpServletRequest request,
            @PathVariable Long conversationId
    ) {
        aiChatService.deleteConversation(getUserId(request), conversationId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(HttpServletRequest request) {
        // JWT 필터가 검증에 성공하면 request에 저장한 사용자 ID를 꺼낸다.
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("인증 토큰이 유효하지 않거나 누락되었습니다.");
        }
        return userId;
    }
}
