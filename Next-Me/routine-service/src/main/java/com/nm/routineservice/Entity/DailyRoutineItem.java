package com.nm.routineservice.Entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
/** 하루 계획 안에서 사용자가 한 번 체크할 수 있는 개별 행동이다. */
@Table(name = "daily_routine_items")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRoutineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private DailyRoutinePlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoutineCategory category;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private LocalTime scheduledTime;

    @Column(nullable = false)
    @Builder.Default
    private int completionWindowMinutes = 30; // scheduledTime부터 완료할 수 있는 제한 시간(분)

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false; // 사용자가 제한 시간 안에 완료했는지 여부

    @Column(nullable = false)
    @Builder.Default
    private boolean missed = false; // 마감 후 false로 확정되어 더는 수정할 수 없는 상태

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void attachTo(DailyRoutinePlan plan) {
        this.plan = plan;
    }

    public void changeCompleted(boolean completed) {
        // missed 항목을 다시 완료 처리하면 보상 계산을 조작할 수 있으므로 차단한다.
        if (missed) {
            throw new IllegalStateException("마감되어 미완료로 확정된 루틴은 수정할 수 없습니다.");
        }
        this.completed = completed;
        this.completedAt = completed ? LocalDateTime.now() : null;
    }

    public void inheritCompletedStatus(LocalDateTime previousCompletedAt) {
        // 기본 루틴을 다시 생성해도 같은 행동의 기존 완료 시각을 유지한다.
        this.missed = false;
        this.completed = true;
        this.completedAt = previousCompletedAt == null
                ? LocalDateTime.now()
                : previousCompletedAt;
    }

    public void markMissed() {
        // 완료하지 않은 항목만 영구 미완료 상태로 잠근다.
        if (!completed) {
            this.missed = true;
            this.completed = false;
            this.completedAt = null;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
