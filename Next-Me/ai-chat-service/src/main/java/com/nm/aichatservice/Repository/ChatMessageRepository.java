package com.nm.aichatservice.Repository;

import com.nm.aichatservice.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 대화 메시지 저장과 시간순 조회를 담당하는 Spring Data JPA 저장소다. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // OpenAI에 전달할 최근 문맥 20건을 최신순으로 조회한다.
    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    // 프론트의 대화 이력 화면용으로 전체 메시지를 오래된 순서부터 조회한다.
    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
