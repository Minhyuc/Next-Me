package com.nm.routineservice.Controller;

import com.nm.routineservice.Dto.AdjustRoutineRequestDto;
import com.nm.routineservice.Dto.DailyRoutineResponseDto;
import com.nm.routineservice.Dto.GenerateRoutineRequestDto;
import com.nm.routineservice.Dto.RoutineProgressResponseDto;
import com.nm.routineservice.Dto.RoutineSettingResponseDto;
import com.nm.routineservice.Dto.RoutineStatsResponseDto;
import com.nm.routineservice.Dto.UpdateRoutineItemRequestDto;
import com.nm.routineservice.Dto.UpdateRoutineSettingRequestDto;
import com.nm.routineservice.Exception.AuthenticationRequiredException;
import com.nm.routineservice.Service.RoutineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
/** JWT 현재 사용자의 하루 루틴 생성, 조회, 완료, 조정, 통계를 제공한다. */
@RequestMapping("/routines/me")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @PostMapping("/today/generate")
    // 프로필과 사용자가 선택한 카테고리를 기준으로 최초 기본 루틴을 만든다.
    public ResponseEntity<DailyRoutineResponseDto> generateToday(
            HttpServletRequest request,
            @Valid @RequestBody(required = false) GenerateRoutineRequestDto dto
    ) {
        return ResponseEntity.ok(routineService.generateToday(
                getUserId(request), request.getHeader("Authorization"), dto
        ));
    }

    @GetMapping("/today")
    // 설정된 초기화 시각을 기준으로 현재 적용 중인 하루 루틴 전체를 반환한다.
    public ResponseEntity<DailyRoutineResponseDto> getToday(HttpServletRequest request) {
        return ResponseEntity.ok(routineService.getToday(getUserId(request)));
    }

    @GetMapping("/today/progress")
    // 진행바에 필요한 완료 개수와 완료율만 가볍게 반환한다.
    public ResponseEntity<RoutineProgressResponseDto> getTodayProgress(HttpServletRequest request) {
        return ResponseEntity.ok(routineService.getTodayProgress(getUserId(request)));
    }

    @PatchMapping("/today/items/{itemId}")
    // 제한 시간 안에서 특정 항목을 완료 또는 완료 취소한다.
    public ResponseEntity<DailyRoutineResponseDto> updateItem(
            HttpServletRequest request,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateRoutineItemRequestDto dto
    ) {
        return ResponseEntity.ok(routineService.updateItem(
                getUserId(request), itemId, dto.getCompleted()
        ));
    }

    @PostMapping("/today/adjust")
    // 갑작스러운 오늘 일정만 AI로 조정하며 다음 날에는 기본 루틴으로 복귀한다.
    public ResponseEntity<DailyRoutineResponseDto> adjustToday(
            HttpServletRequest request,
            @Valid @RequestBody AdjustRoutineRequestDto dto
    ) {
        return ResponseEntity.ok(routineService.adjustToday(
                getUserId(request), request.getHeader("Authorization"), dto
        ));
    }

    @GetMapping("/settings")
    // 사용자별 루틴 초기화 시각과 시간대를 조회한다.
    public ResponseEntity<RoutineSettingResponseDto> getSettings(HttpServletRequest request) {
        return ResponseEntity.ok(routineService.getSetting(getUserId(request)));
    }

    @PutMapping("/settings")
    // 사용자가 직접 초기화 시각을 바꿀 때 호출한다.
    public ResponseEntity<RoutineSettingResponseDto> updateSettings(
            HttpServletRequest request,
            @Valid @RequestBody UpdateRoutineSettingRequestDto dto
    ) {
        return ResponseEntity.ok(routineService.updateSetting(getUserId(request), dto));
    }

    @GetMapping("/stats")
    // 하루 루틴을 100% 완료한 날짜와 누적 횟수를 반환한다.
    public ResponseEntity<RoutineStatsResponseDto> getStats(HttpServletRequest request) {
        return ResponseEntity.ok(routineService.getStats(getUserId(request)));
    }

    private Long getUserId(HttpServletRequest request) {
        // JWT 필터가 검증해 둔 userId만 사용하므로 Body에서 사용자 ID를 받지 않는다.
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new AuthenticationRequiredException("인증 토큰이 유효하지 않거나 누락되었습니다.");
        }
        return userId;
    }
}
