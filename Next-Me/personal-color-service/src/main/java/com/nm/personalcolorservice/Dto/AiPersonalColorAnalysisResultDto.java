package com.nm.personalcolorservice.Dto;

/** 퍼스널컬러 분석 대화에서 OpenAI가 반환한 구조화 결과다. */
public record AiPersonalColorAnalysisResultDto(
        String reply,          // 사용자에게 보여줄 다음 질문 또는 분석 결과
        boolean completed,     // 분석에 필요한 정보 수집이 끝났는지 여부
        String personalColor,  // 완료 시 판정한 퍼스널컬러
        Integer confidence     // AI가 판단한 신뢰도(0~100)
) {
}
