package com.nm.routineservice.Dto;

import com.nm.routineservice.Entity.RoutineCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
/** user-service의 프로필과 선택한 루틴 종류를 AI 생성부에 전달한다. */
public class UserRoutineContextDto {
    private String profileJson; // 사용자 기본 프로필 원본 JSON
    private String preferenceJson; // 피드백 받고 싶은 루틴 등 선호 정보 JSON
    private Set<RoutineCategory> selectedCategories; // AI가 생성해도 되는 루틴 카테고리
}
