package com.nm.userservice.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId; // login-service의 사용자 ID

    private String nickname;
    private String gender;
    private Integer ageGroup;

    private String workDay;
    private String leaveTime;
    private String comeTime;
    private String workStyle;

    private Integer sleepHour;

    private String makeupFrequency;
    private String sunscreenFrequency;

    private Integer exerciseCount;
    private String sweatAmount;

    private String skinType;
    private String concerns;
    private String personalColor;

    private String onboardingStatus; // INCOMPLETE, COMPLETE
    private String onboardingMode;   // GUIDED, DIRECT
    private Integer onboardingStep;  // 몇 단계까지 진행했는지

    private Integer skinTypeConfidence;
    private Integer personalColorConfidence;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.onboardingStatus == null) {
            this.onboardingStatus = "INCOMPLETE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}