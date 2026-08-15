package com.nm.usercareservice.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_care_recommendations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 사용자 프로필을 기준으로 생성한 세안·스킨케어 성분 추천 한 건을 저장한다. */
public class UserCareRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skin_type", nullable = false, length = 100)
    private String skinType;

    @Column(length = 1000)
    private String concerns;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Lob
    @Column(name = "recommended_ingredients_json", nullable = false, columnDefinition = "LONGTEXT")
    private String recommendedIngredientsJson;

    @Lob
    @Column(name = "caution_ingredients_json", nullable = false, columnDefinition = "LONGTEXT")
    private String cautionIngredientsJson;

    @Column(nullable = false, length = 2000)
    private String disclaimer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
