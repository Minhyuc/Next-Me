package com.nm.userservice.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_routine_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoutinePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // 세안 차트
    @Builder.Default
    private Boolean routineCleansing = false;

    @Builder.Default
    private Boolean routineSkinCare = false;

    // 화장 차트
    @Builder.Default
    private Boolean routinePersonalColor = false;

    // 루틴 차트
    @Builder.Default
    private Boolean routineSleepWake = false;

    @Builder.Default
    private Boolean routineDiet = false;

    @Builder.Default
    private Boolean routineExercise = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}