package com.nm.characterservice.Service;

import com.nm.characterservice.Dto.CharacterCatalogItemDto;
import com.nm.characterservice.Dto.CharacterCatalogResponseDto;
import com.nm.characterservice.Dto.CharacterCollectionResponseDto;
import com.nm.characterservice.Dto.CharacterRewardResponseDto;
import com.nm.characterservice.Entity.CharacterRewardClaim;
import com.nm.characterservice.Entity.OwnedCharacter;
import com.nm.characterservice.Repository.CharacterRewardClaimRepository;
import com.nm.characterservice.Repository.OwnedCharacterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Service
/** 캐릭터 지급, 보유 현황, 전체 도감을 관리하는 핵심 서비스다. */
public class CharacterService {

    private final OwnedCharacterRepository ownedCharacterRepository;
    private final CharacterRewardClaimRepository rewardClaimRepository;
    private final int catalogSize;
    private final SecureRandom random = new SecureRandom();

    public CharacterService(
            OwnedCharacterRepository ownedCharacterRepository,
            CharacterRewardClaimRepository rewardClaimRepository,
            @Value("${character.catalog-size:3}") int catalogSize
    ) {
        if (catalogSize < 1) {
            throw new IllegalArgumentException("캐릭터 도감 크기는 1 이상이어야 합니다.");
        }
        this.ownedCharacterRepository = ownedCharacterRepository;
        this.rewardClaimRepository = rewardClaimRepository;
        this.catalogSize = catalogSize;
    }

    @Transactional
    public CharacterRewardResponseDto rewardForCompletedRoutine(
            Long userId,
            LocalDate routineDate
    ) {
        // 동일 날짜의 재요청은 기존 지급 결과를 반환하여 캐릭터 재추첨을 막는다.
        var existingClaim = rewardClaimRepository.findByUserIdAndRoutineDate(userId, routineDate);
        if (existingClaim.isPresent()) {
            return toAlreadyProcessedResponse(existingClaim.get(), loadOwned(userId));
        }

        List<OwnedCharacter> ownedCharacters = loadOwned(userId);
        Set<Integer> ownedNumbers = new HashSet<>();
        ownedCharacters.forEach(character -> ownedNumbers.add(character.getCharacterNumber()));

        List<Integer> unownedNumbers = IntStream.rangeClosed(1, catalogSize)
                .filter(number -> !ownedNumbers.contains(number))
                .boxed()
                .toList();

        LocalDateTime now = LocalDateTime.now();
        if (unownedNumbers.isEmpty()) {
            rewardClaimRepository.save(CharacterRewardClaim.builder()
                    .userId(userId)
                    .routineDate(routineDate)
                    .characterNumber(null)
                    .processedAt(now)
                    .build());
            return CharacterRewardResponseDto.builder()
                    .rewardProcessed(true)
                    .alreadyProcessed(false)
                    .newCharacterCollected(false)
                    .characterNumber(null)
                    .ownedCount(catalogSize)
                    .totalCharacterCount(catalogSize)
                    .allCharactersCollected(true)
                    .message("모든 캐릭터를 이미 수집했습니다.")
                    .build();
        }

        // 보유하지 않은 번호만 후보로 사용하므로 중복 캐릭터는 절대 지급되지 않는다.
        int selectedNumber = unownedNumbers.get(random.nextInt(unownedNumbers.size()));
        ownedCharacterRepository.save(OwnedCharacter.builder()
                .userId(userId)
                .characterNumber(selectedNumber)
                .collectedAt(now)
                .build());
        rewardClaimRepository.save(CharacterRewardClaim.builder()
                .userId(userId)
                .routineDate(routineDate)
                .characterNumber(selectedNumber)
                .processedAt(now)
                .build());

        int ownedCount = ownedCharacters.size() + 1;
        return CharacterRewardResponseDto.builder()
                .rewardProcessed(true)
                .alreadyProcessed(false)
                .newCharacterCollected(true)
                .characterNumber(selectedNumber)
                .ownedCount(ownedCount)
                .totalCharacterCount(catalogSize)
                .allCharactersCollected(ownedCount == catalogSize)
                .message(selectedNumber + "번 캐릭터를 새로 수집했습니다.")
                .build();
    }

    @Transactional(readOnly = true)
    public CharacterCollectionResponseDto getCollection(Long userId) {
        List<Integer> numbers = loadOwned(userId).stream()
                .map(OwnedCharacter::getCharacterNumber)
                .toList();
        return CharacterCollectionResponseDto.builder()
                .totalCharacterCount(catalogSize)
                .ownedCount(numbers.size())
                .allCharactersCollected(numbers.size() == catalogSize)
                .ownedCharacterNumbers(numbers)
                .build();
    }

    @Transactional(readOnly = true)
    public CharacterCatalogResponseDto getCatalog(Long userId) {
        List<OwnedCharacter> ownedCharacters = loadOwned(userId);
        Map<Integer, OwnedCharacter> ownedByNumber = new HashMap<>();
        ownedCharacters.forEach(character ->
                ownedByNumber.put(character.getCharacterNumber(), character));

        List<CharacterCatalogItemDto> catalog = new ArrayList<>();
        for (int number = 1; number <= catalogSize; number++) {
            OwnedCharacter owned = ownedByNumber.get(number);
            catalog.add(CharacterCatalogItemDto.builder()
                    .characterNumber(number)
                    .owned(owned != null)
                    .collectedAt(owned == null ? null : owned.getCollectedAt())
                    .build());
        }

        return CharacterCatalogResponseDto.builder()
                .totalCharacterCount(catalogSize)
                .ownedCount(ownedCharacters.size())
                .allCharactersCollected(ownedCharacters.size() == catalogSize)
                .characters(catalog)
                .build();
    }

    private CharacterRewardResponseDto toAlreadyProcessedResponse(
            CharacterRewardClaim claim,
            List<OwnedCharacter> ownedCharacters
    ) {
        boolean allCollected = ownedCharacters.size() == catalogSize;
        String message = claim.getCharacterNumber() == null
                ? "이 날짜의 보상은 이미 처리되었으며 모든 캐릭터를 보유 중입니다."
                : "이 날짜에는 이미 " + claim.getCharacterNumber() + "번 캐릭터가 지급되었습니다.";
        return CharacterRewardResponseDto.builder()
                .rewardProcessed(true)
                .alreadyProcessed(true)
                .newCharacterCollected(false)
                .characterNumber(claim.getCharacterNumber())
                .ownedCount(ownedCharacters.size())
                .totalCharacterCount(catalogSize)
                .allCharactersCollected(allCollected)
                .message(message)
                .build();
    }

    private List<OwnedCharacter> loadOwned(Long userId) {
        return ownedCharacterRepository.findAllByUserIdOrderByCharacterNumberAsc(userId);
    }
}
