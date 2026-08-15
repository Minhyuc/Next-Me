package com.nm.characterservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 1번부터 3번까지 순서대로 내려주는 전체 캐릭터 도감 응답이다. */
public class CharacterCatalogResponseDto {
    private int totalCharacterCount;
    private int ownedCount;
    private boolean allCharactersCollected;
    private List<CharacterCatalogItemDto> characters;
}
