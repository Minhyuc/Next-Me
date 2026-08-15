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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "owned_characters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_owned_character_user_number",
                columnNames = {"user_id", "character_number"}
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 사용자가 실제로 수집한 캐릭터 번호를 한 번만 저장한다. */
public class OwnedCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "character_number", nullable = false)
    private int characterNumber;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
