package com.nm.characterservice.Controller;

import com.nm.characterservice.Dto.CharacterRewardRequestDto;
import com.nm.characterservice.Dto.CharacterRewardResponseDto;
import com.nm.characterservice.Service.CharacterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/characters")
/** 외부 사용자가 아닌 routine-service가 100% 완료 보상을 요청하는 내부 API다. */
public class InternalCharacterController {

    private final CharacterService characterService;
    private final String internalKey;

    public InternalCharacterController(
            CharacterService characterService,
            @Value("${character.internal-key}") String internalKey
    ) {
        this.characterService = characterService;
        this.internalKey = internalKey;
    }

    @PostMapping("/routine-completions")
    public ResponseEntity<CharacterRewardResponseDto> rewardRoutineCompletion(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String requestKey,
            @Valid @RequestBody CharacterRewardRequestDto request
    ) {
        validateInternalKey(requestKey);
        return ResponseEntity.ok(characterService.rewardForCompletedRoutine(
                request.getUserId(), request.getRoutineDate()
        ));
    }

    private void validateInternalKey(String requestKey) {
        if (requestKey == null || !MessageDigest.isEqual(
                internalKey.getBytes(StandardCharsets.UTF_8),
                requestKey.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new SecurityException("캐릭터 보상 내부 API에 접근할 수 없습니다.");
        }
    }
}
