package com.nm.aichatservice.Repository;

import com.nm.aichatservice.Entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 대화방 저장과 사용자별 조회를 담당하는 Spring Data JPA 저장소다. */
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    // ID와 userId를 함께 검사하여 다른 사용자의 대화방 접근을 막는다.
    Optional<ChatConversation> findByIdAndUserId(Long id, Long userId);

    // 대화방 목록 화면에서 최근 대화를 먼저 보여준다.
    List<ChatConversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
}
