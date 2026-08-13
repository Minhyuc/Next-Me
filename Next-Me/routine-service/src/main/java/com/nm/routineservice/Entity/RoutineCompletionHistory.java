package com.nm.routineservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
/** 하루 계획이 처음 100%가 된 시점을 보상·캐릭터 지급 판단용으로 한 번만 저장한다. */
@Table(name = "routine_completion_history",
        uniqueConstraints = @UniqueConstraint(columnNames = "plan_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineCompletionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate completedDate;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private long completionNumber; // 해당 사용자에게 누적 몇 번째 100% 완료인지 표시
}
