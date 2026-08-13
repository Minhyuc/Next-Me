package com.nm.personalcolorservice.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 기존 통합 추천 테이블의 데이터를 새 옷·화장 테이블로 한 번씩 복사한다.
 * 기존 테이블은 자동 삭제하지 않으므로 이전 결과를 확인하거나 복구할 수 있다.
 */
@Component
public class LegacyRecommendationMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyRecommendationMigration.class);
    private static final String LEGACY_TABLE = "personal_color_recommendations";

    private final JdbcTemplate jdbcTemplate;

    public LegacyRecommendationMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!legacyTableExists()) {
            return;
        }

        try {
            int outfits = migrateOutfits();
            int makeup = migrateMakeup();
            if (outfits > 0 || makeup > 0) {
                log.info("기존 퍼스널컬러 추천 이전 완료: 옷 {}건, 화장 {}건", outfits, makeup);
            }
        } catch (DataAccessException exception) {
            // 이전 테이블의 예상 구조가 다르더라도 신규 서비스 실행 자체는 계속한다.
            log.warn("기존 추천 데이터를 자동 이전하지 못했습니다. 기존 테이블은 그대로 유지됩니다.", exception);
        }
    }

    private boolean legacyTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                LEGACY_TABLE
        );
        return count != null && count > 0;
    }

    private int migrateOutfits() {
        if (!legacyColumnExists("outfits_json")) {
            return 0;
        }
        // legacy_recommendation_id의 UNIQUE 인덱스로 재시작 시 같은 행이 중복 이전되지 않는다.
        return jdbcTemplate.update("""
                INSERT INTO outfit_recommendations
                    (user_id, recommendation_date, personal_color, summary,
                     request_json, outfits_json, created_at, legacy_recommendation_id)
                SELECT old.user_id, old.recommendation_date, old.personal_color, old.summary,
                       old.request_json, old.outfits_json, COALESCE(old.created_at, CURRENT_TIMESTAMP), old.id
                FROM personal_color_recommendations old
                WHERE old.outfits_json IS NOT NULL
                  AND TRIM(old.outfits_json) NOT IN ('', '[]', 'null')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM outfit_recommendations moved
                      WHERE moved.legacy_recommendation_id = old.id
                  )
                """);
    }

    private int migrateMakeup() {
        if (!legacyColumnExists("cosmetics_json")) {
            return 0;
        }
        return jdbcTemplate.update("""
                INSERT INTO makeup_recommendations
                    (user_id, recommendation_date, personal_color, summary,
                     request_json, cosmetics_json, created_at, legacy_recommendation_id)
                SELECT old.user_id, old.recommendation_date, old.personal_color, old.summary,
                       old.request_json, old.cosmetics_json, COALESCE(old.created_at, CURRENT_TIMESTAMP), old.id
                FROM personal_color_recommendations old
                WHERE old.cosmetics_json IS NOT NULL
                  AND TRIM(old.cosmetics_json) NOT IN ('', '[]', 'null')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM makeup_recommendations moved
                      WHERE moved.legacy_recommendation_id = old.id
                  )
                """);
    }

    private boolean legacyColumnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                LEGACY_TABLE,
                columnName
        );
        return count != null && count > 0;
    }
}
