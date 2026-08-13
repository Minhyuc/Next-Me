package com.nm.personalcolorservice.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
/** 사용자별 퍼스널컬러 분석 대화방과 최종 판정 상태를 저장한다. */
@Table(
        name = "personal_color_conversations",
        indexes = @Index(
                name = "idx_pc_conversation_user_updated",
                columnList = "user_id, updated_at"
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalColorConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // login-service가 발급한 사용자 식별자다. 서비스 간에는 이 값을 공통으로 사용한다.
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE: 진행 중, COMPLETED: 판정 완료

    @Column(length = 30)
    private String personalColor; // 완료 전 null, 완료 후 봄 웜/여름 쿨/가을 웜/겨울 쿨

    private Integer confidence; // AI 판정 신뢰도 0~100

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    // 대화방 삭제 시 소속 메시지도 함께 제거한다.
    @Builder.Default
    private List<PersonalColorMessage> messages = new ArrayList<>();

    public void complete(String personalColor, Integer confidence) {
        // 최종 판정이 나왔을 때만 완료 상태와 결과를 기록한다.
        this.status = "COMPLETED";
        this.personalColor = personalColor;
        this.confidence = confidence;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
