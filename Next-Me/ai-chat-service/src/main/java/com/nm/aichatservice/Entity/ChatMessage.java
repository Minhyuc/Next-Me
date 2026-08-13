package com.nm.aichatservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 사용자 질문 또는 AI 답변 한 건을 저장하는 DB 엔티티다. */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 메시지 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation; // 이 메시지가 속한 대화방

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role; // 발신자가 사용자(USER)인지 AI(ASSISTANT)인지 구분

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 길이 제한 없이 저장하는 질문 또는 답변 본문

    @Column(nullable = false)
    private boolean outOfScope; // 범위 밖 질문에 대한 고정 안내 메시지이면 true

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 메시지 생성 시각

    @PrePersist
    protected void onCreate() {
        // DB에 처음 저장되기 직전에 현재 시각을 기록한다.
        this.createdAt = LocalDateTime.now();
    }
}
