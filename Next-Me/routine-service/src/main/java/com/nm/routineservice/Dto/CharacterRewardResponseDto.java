package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** character-service가 반환한 캐릭터 지급 결과를 루틴 완료 응답에 포함한다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
