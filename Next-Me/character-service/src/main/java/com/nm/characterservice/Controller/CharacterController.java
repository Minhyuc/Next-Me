package com.nm.characterservice.Controller;

import com.nm.characterservice.Dto.CharacterCatalogResponseDto;
import com.nm.characterservice.Dto.CharacterCollectionResponseDto;
import com.nm.characterservice.Exception.AuthenticationRequiredException;
import com.nm.characterservice.Service.CharacterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/characters/me")
/** JWT 현재 사용자의 캐릭터 보유 현황과 전체 도감을 제공한다. */
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping("/collection")
    public ResponseEntity<CharacterCollectionResponseDto> getCollection(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(characterService.getCollection(getUserId(request)));
    }

    @GetMapping("/catalog")
    public ResponseEntity<CharacterCatalogResponseDto> getCatalog(HttpServletRequest request) {
        return ResponseEntity.ok(characterService.getCatalog(getUserId(request)));
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new AuthenticationRequiredException("인증 토큰이 유효하지 않거나 누락되었습니다.");
        }
        return userId;
    }
}
