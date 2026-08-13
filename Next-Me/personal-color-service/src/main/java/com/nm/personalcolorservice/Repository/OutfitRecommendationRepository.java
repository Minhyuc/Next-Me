package com.nm.personalcolorservice.Repository;

import com.nm.personalcolorservice.Entity.OutfitRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/** 옷 추천만 저장하고 사용자별 최신 추천과 페이지 이력을 조회한다. */
public interface OutfitRecommendationRepository extends JpaRepository<OutfitRecommendation, Long> {

    // 오늘 생성한 옷 추천 중 가장 최근 한 건을 반환한다.
    Optional<OutfitRecommendation> findFirstByUserIdAndRecommendationDateOrderByCreatedAtDesc(
            Long userId,
            LocalDate recommendationDate
    );

    // 현재 사용자의 옷 추천 이력을 최신순으로 페이지 조회한다.
    Page<OutfitRecommendation> findAllByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );
}
