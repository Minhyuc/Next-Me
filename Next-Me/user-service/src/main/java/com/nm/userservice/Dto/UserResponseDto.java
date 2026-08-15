package com.nm.userservice.Dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long userId;

    // 프론트는 이 두 값으로 최초 프로필 작성 화면 진입 여부를 판단한다.
    // profileEmpty=true: 아직 실제 프로필 값이 하나도 저장되지 않은 상태
    // profileCompleted=true: 서비스 이용에 필요한 프로필 값이 모두 작성된 상태
    private boolean profileEmpty;
    private boolean profileCompleted;
    private String onboardingStatus;
    private String onboardingMode;
    private Integer onboardingStep;

    private String nickname;
    private String gender;
    private Integer ageGroup;

    private String workDay;
    private String leaveTime;
    private String comeTime;
    private String workStyle;

    private Integer sleepHour;

    private String makeupFrequency;
    private String sunscreenFrequency;
    private Integer exerciseCount;
    private String sweatAmount;

    private String skinType;
    private String concerns;

    private String personalColor;


    //사진 점수
    private Integer skinTypeConfidence;
    private Integer personalColorConfidence;
}
