package com.nm.userservice.Dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long userId;
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