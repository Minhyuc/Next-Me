package com.nm.personalcolorservice.Repository;

import com.nm.personalcolorservice.Entity.PersonalColorMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalColorMessageRepository extends JpaRepository<PersonalColorMessage, Long> {

    // OpenAI에는 자연스러운 문맥을 위해 오래된 메시지부터 전달한다.
    List<PersonalColorMessage> findAllByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Page<PersonalColorMessage> findAllByConversationIdOrderByCreatedAtDesc(
            Long conversationId,
            Pageable pageable
    );
}
    // 프론트 이력은 최신 메시지를 빠르게 볼 수 있도록 최신순 페이지 조회한다.
