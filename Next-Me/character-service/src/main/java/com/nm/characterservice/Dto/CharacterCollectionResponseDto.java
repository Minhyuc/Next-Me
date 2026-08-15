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
/** 현재 사용자가 보유한 캐릭터 번호만 빠르게 확인하는 응답이다. */
public class CharacterCollectionResponseDto {
    private int totalCharacterCount;
    private int ownedCount;
    private boolean allCharactersCollected;
    private List<Integer> ownedCharacterNumbers;
}
