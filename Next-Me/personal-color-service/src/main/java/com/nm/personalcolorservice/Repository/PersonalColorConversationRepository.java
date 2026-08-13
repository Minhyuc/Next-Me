package com.nm.personalcolorservice.Repository;

import com.nm.personalcolorservice.Entity.PersonalColorConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalColorConversationRepository
        extends JpaRepository<PersonalColorConversation, Long> {

    // 대화 ID뿐 아니라 userId도 확인하여 타인 대화 접근을 막는다.
    Optional<PersonalColorConversation> findByIdAndUserId(Long id, Long userId);

    Optional<PersonalColorConversation> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(
            Long userId, String status
    );

    Page<PersonalColorConversation> findAllByUserIdOrderByUpdatedAtDesc(
            Long userId,
            Pageable pageable
    );
}
    // 사용자의 가장 최근 진행 중 대화를 찾아 이어서 사용할 때 호출한다.
    // 최근 갱신된 대화부터 페이지 단위로 조회한다.
