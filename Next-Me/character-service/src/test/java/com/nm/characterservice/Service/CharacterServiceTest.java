package com.nm.characterservice.Service;

import com.nm.characterservice.Dto.CharacterCatalogResponseDto;
import com.nm.characterservice.Dto.CharacterCollectionResponseDto;
import com.nm.characterservice.Dto.CharacterRewardResponseDto;
import com.nm.characterservice.Entity.CharacterRewardClaim;
import com.nm.characterservice.Entity.OwnedCharacter;
import com.nm.characterservice.Repository.CharacterRewardClaimRepository;
import com.nm.characterservice.Repository.OwnedCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CharacterServiceTest {

    private OwnedCharacterRepository ownedRepository;
    private CharacterRewardClaimRepository claimRepository;
    private CharacterService characterService;

    @BeforeEach
    void setUp() {
        ownedRepository = mock(OwnedCharacterRepository.class);
        claimRepository = mock(CharacterRewardClaimRepository.class);
        characterService = new CharacterService(ownedRepository, claimRepository, 3);
    }

    @Test
    void rewardSelectsOnlyFromUnownedCharacters() {
        LocalDate routineDate = LocalDate.of(2026, 8, 14);
        when(claimRepository.findByUserIdAndRoutineDate(1L, routineDate))
                .thenReturn(Optional.empty());
        when(ownedRepository.findAllByUserIdOrderByCharacterNumberAsc(1L))
                .thenReturn(List.of(
                        owned(1L, 1),
                        owned(1L, 2)
                ));

        CharacterRewardResponseDto response =
                characterService.rewardForCompletedRoutine(1L, routineDate);

        assertTrue(response.isRewardProcessed());
        assertTrue(response.isNewCharacterCollected());
        assertEquals(3, response.getCharacterNumber());
        assertEquals(3, response.getOwnedCount());
        assertTrue(response.isAllCharactersCollected());

        ArgumentCaptor<OwnedCharacter> characterCaptor =
                ArgumentCaptor.forClass(OwnedCharacter.class);
        verify(ownedRepository).save(characterCaptor.capture());
        assertEquals(3, characterCaptor.getValue().getCharacterNumber());
    }

    @Test
    void sameRoutineDateReturnsExistingClaimWithoutAnotherCharacter() {
        LocalDate routineDate = LocalDate.of(2026, 8, 14);
        when(claimRepository.findByUserIdAndRoutineDate(1L, routineDate))
                .thenReturn(Optional.of(CharacterRewardClaim.builder()
                        .userId(1L)
                        .routineDate(routineDate)
                        .characterNumber(2)
                        .processedAt(LocalDateTime.now())
                        .build()));
        when(ownedRepository.findAllByUserIdOrderByCharacterNumberAsc(1L))
                .thenReturn(List.of(owned(1L, 2)));

        CharacterRewardResponseDto response =
                characterService.rewardForCompletedRoutine(1L, routineDate);

        assertTrue(response.isAlreadyProcessed());
        assertFalse(response.isNewCharacterCollected());
        assertEquals(2, response.getCharacterNumber());
        verify(ownedRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(claimRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allOwnedRecordsTheDateWithoutIssuingDuplicateCharacter() {
        LocalDate routineDate = LocalDate.of(2026, 8, 14);
        when(claimRepository.findByUserIdAndRoutineDate(1L, routineDate))
                .thenReturn(Optional.empty());
        when(ownedRepository.findAllByUserIdOrderByCharacterNumberAsc(1L))
                .thenReturn(List.of(owned(1L, 1), owned(1L, 2), owned(1L, 3)));

        CharacterRewardResponseDto response =
                characterService.rewardForCompletedRoutine(1L, routineDate);

        assertFalse(response.isNewCharacterCollected());
        assertNull(response.getCharacterNumber());
        assertTrue(response.isAllCharactersCollected());
        verify(ownedRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(claimRepository).save(org.mockito.ArgumentMatchers.argThat(
                claim -> claim.getCharacterNumber() == null
                        && routineDate.equals(claim.getRoutineDate())
        ));
    }

    @Test
    void collectionAndCatalogAreSortedAndExposeOwnershipFlags() {
        when(ownedRepository.findAllByUserIdOrderByCharacterNumberAsc(1L))
                .thenReturn(List.of(owned(1L, 1), owned(1L, 3)));

        CharacterCollectionResponseDto collection = characterService.getCollection(1L);
        CharacterCatalogResponseDto catalog = characterService.getCatalog(1L);

        assertEquals(List.of(1, 3), collection.getOwnedCharacterNumbers());
        assertEquals(2, collection.getOwnedCount());
        assertEquals(3, catalog.getCharacters().size());
        assertTrue(catalog.getCharacters().get(0).isOwned());
        assertFalse(catalog.getCharacters().get(1).isOwned());
        assertTrue(catalog.getCharacters().get(2).isOwned());
    }

    private OwnedCharacter owned(Long userId, int characterNumber) {
        return OwnedCharacter.builder()
                .userId(userId)
                .characterNumber(characterNumber)
                .collectedAt(LocalDateTime.of(2026, 8, 14, 12, characterNumber))
                .build();
    }
}
