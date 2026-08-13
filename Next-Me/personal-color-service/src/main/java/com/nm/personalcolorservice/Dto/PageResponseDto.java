package com.nm.personalcolorservice.Dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Spring의 Page 객체를 프론트에서 재사용하기 쉬운 고정 응답 형식으로 변환한다. */
public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
    public static <S, T> PageResponseDto<T> from(
            Page<S> source,
            Function<S, T> mapper
    ) {
        // Entity를 외부 DTO로 매핑하면서 페이지 메타데이터는 그대로 유지한다.
        return new PageResponseDto<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast(),
                source.isEmpty()
        );
    }
}
