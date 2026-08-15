package com.nm.characterservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 루틴 100% 완료 후 신규 캐릭터 지급 여부를 알려주는 응답이다. */
public class CharacterRewardResponseDto {
    private boolean rewardProcessed;
    private boolean alreadyProcessed;
    private boolean newCharacterCollected;
    private Integer characterNumber;
    private int ownedCount;
    private int totalCharacterCount;
    private boolean allCharactersCollected;
    private String message;
}
