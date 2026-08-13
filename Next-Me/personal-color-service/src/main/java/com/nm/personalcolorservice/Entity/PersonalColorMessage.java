package com.nm.personalcolorservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
/** 퍼스널컬러 분석 과정의 사용자/AI 메시지 한 건을 저장한다. */
@Table(
        name = "personal_color_messages",
        indexes = @Index(
                name = "idx_pc_message_conversation_created",
                columnList = "conversation_id, created_at"
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalColorMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // 실제로 대화방이 필요할 때만 조회하도록 지연 로딩한다.
    @JoinColumn(name = "conversation_id", nullable = false)
    private PersonalColorConversation conversation;

    @Column(nullable = false, length = 20)
    private String role; // USER 또는 ASSISTANT

    @Column(nullable = false, length = 3000)
    private String content; // 채팅창에 그대로 표시할 메시지 본문

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
