package com.nm.routineservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
/** AI 또는 사용자가 정한 사용자별 하루 루틴 경계 시각과 시간대를 저장한다. */
@Table(name = "routine_settings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private LocalTime resetTime; // 달력의 00시가 아닌 이 사용자 루틴의 하루 시작 경계

    @Column(nullable = false, length = 50)
    private String zoneId; // 예: Asia/Seoul

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void update(LocalTime resetTime, String zoneId) {
        this.resetTime = resetTime;
        this.zoneId = zoneId;
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
