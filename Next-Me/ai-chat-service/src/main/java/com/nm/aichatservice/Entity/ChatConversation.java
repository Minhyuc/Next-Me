package com.nm.aichatservice.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 사용자 한 명의 AI 대화방과 그 메시지 목록을 저장하는 DB 엔티티다. */
@Entity
@Table(name = "chat_conversations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB가 자동 생성하는 대화방 ID

    @Column(nullable = false)
    private Long userId; // JWT에서 확인한 대화방 소유자 ID

    @Column(nullable = false, length = 100)
    private String title; // 첫 질문을 최대 40자로 줄여 만든 제목

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>(); // 대화방 삭제 시 함께 삭제되는 메시지

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 최초 생성 시각

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 마지막 대화 시각

    @PrePersist
    protected void onCreate() {
        // INSERT 직전에 생성·수정 시각을 같은 값으로 자동 입력한다.
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void touch() {
        // 새 메시지가 저장될 때 대화방을 최근 목록의 위로 올리기 위한 시각 갱신이다.
        this.updatedAt = LocalDateTime.now();
    }
}
