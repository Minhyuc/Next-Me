package com.nm.routineservice.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
/** 한 사용자에게 특정 routineDate 동안 적용되는 루틴 항목 묶음이다. */
@Table(name = "daily_routine_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "routine_date"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRoutinePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "routine_date", nullable = false)
    private LocalDate routineDate;

    @Column(nullable = false)
    private LocalTime resetTimeSnapshot; // 이 계획 생성 당시 적용된 초기화 시각

    @Column(nullable = false)
    @Builder.Default
    private int completionPercentage = 0; // items의 완료 비율을 반올림한 0~100 값

    private LocalDateTime firstCompletedAt;

    @Column(length = 500)
    private String adjustmentSummary;

    /**
     * AI로 최초 생성한 기본 루틴의 JSON 스냅샷입니다.
     * 당일 일정 조정으로 items가 변경되어도 이 값은 유지되며,
     * 다음 루틴 날짜가 시작될 때 기본 루틴으로 복귀하는 데 사용합니다.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Builder.Default
    private String baselineItemsJson = "[]";

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyRoutineItem> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void addItem(DailyRoutineItem item) {
        item.attachTo(this);
        this.items.add(item);
    }

    public void replaceItems(List<DailyRoutineItem> replacement) {
        // orphanRemoval로 기존 항목을 제거하고 조정 결과를 새 목록으로 연결한다.
        this.items.clear();
        replacement.forEach(this::addItem);
    }

    public void updateProgress() {
        // 항목 상태가 바뀔 때마다 완료율과 최초 100% 달성 시각을 함께 계산한다.
        if (items.isEmpty()) {
            this.completionPercentage = 0;
            return;
        }
        long completedCount = items.stream().filter(DailyRoutineItem::isCompleted).count();
        this.completionPercentage = (int) Math.round(completedCount * 100.0 / items.size());
        if (completedCount == items.size() && firstCompletedAt == null) {
            this.firstCompletedAt = LocalDateTime.now();
        }
    }

    public void updateAdjustmentSummary(String summary) {
        this.adjustmentSummary = summary;
    }

    public void updateBaselineItemsJson(String baselineItemsJson) {
        this.baselineItemsJson = baselineItemsJson;
    }

    public void updateResetTimeSnapshot(LocalTime resetTime) {
        this.resetTimeSnapshot = resetTime;
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
