package com.nm.usercareservice.Controller;

import com.nm.usercareservice.Dto.UserCareRecommendationResponseDto;
import com.nm.usercareservice.Exception.AuthenticationRequiredException;
import com.nm.usercareservice.Service.UserCareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-care/me/recommendations")
/** JWT 현재 사용자의 피부 관리 성분 추천 생성과 최신 결과 조회를 제공한다. */
public class UserCareController {

    private final UserCareService userCareService;

    public UserCareController(UserCareService userCareService) {
        this.userCareService = userCareService;
    }

    @PostMapping
    public ResponseEntity<UserCareRecommendationResponseDto> generate(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(userCareService.generate(
                getUserId(request), request.getHeader("Authorization")
        ));
    }

    @GetMapping("/latest")
    public ResponseEntity<UserCareRecommendationResponseDto> getLatest(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(userCareService.getLatest(getUserId(request)));
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new AuthenticationRequiredException("인증 토큰이 유효하지 않거나 누락되었습니다.");
        }
        return userId;
    }
}
