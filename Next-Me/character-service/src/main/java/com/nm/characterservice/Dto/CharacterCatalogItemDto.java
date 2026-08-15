package com.nm.characterservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 도감의 캐릭터 번호별 보유 여부와 획득 시각을 나타낸다. */
public class CharacterCatalogItemDto {
    private int characterNumber;
    private boolean owned;
    private LocalDateTime collectedAt;
}
