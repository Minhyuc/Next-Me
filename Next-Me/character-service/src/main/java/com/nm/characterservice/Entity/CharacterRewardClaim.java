package com.nm.characterservice.Entity;

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
@Table(
        name = "character_reward_claims",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_character_reward_user_date",
                columnNames = {"user_id", "routine_date"}
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 같은 사용자의 같은 루틴 날짜에 보상이 중복 지급되는 것을 막는 이력이다. */
public class CharacterRewardClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "routine_date", nullable = false)
    private LocalDate routineDate;

    // 이미 1~3번을 모두 보유한 경우에는 지급할 번호가 없어 null로 기록한다.
    @Column(name = "character_number")
    private Integer characterNumber;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
