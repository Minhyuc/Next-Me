package com.nm.personalcolorservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** AI가 생성한 옷 추천 한 건과 당시 사용자의 요청을 저장한다. */
@Entity
@Table(
        name = "outfit_recommendations",
        indexes = {
                @Index(
                        name = "idx_outfit_recommendation_user_date_created",
                        columnList = "user_id, recommendation_date, created_at"
                ),
                @Index(
                        name = "uk_outfit_legacy_recommendation",
                        columnList = "legacy_recommendation_id",
                        unique = true
                )
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 옷 추천 이력 ID

    @Column(nullable = false)
    private Long userId; // JWT에서 확인한 추천 소유자 ID

    @Column(nullable = false)
    private LocalDate recommendationDate; // 사용자의 시간대를 기준으로 한 추천 날짜

    @Column(nullable = false, length = 50)
    private String personalColor; // 추천 생성 당시 적용한 퍼스널컬러

    @Column(nullable = false, length = 500)
    private String summary; // 전체 코디 방향 요약

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String requestJson; // 사용자가 입력한 자연어 요청 JSON

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String outfitsJson; // AI가 생성한 코디 목록 JSON

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 추천 생성 시각

    @Column(name = "legacy_recommendation_id")
    private Long legacyRecommendationId; // 기존 통합 테이블에서 이전된 행의 원본 ID

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
