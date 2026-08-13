package com.nm.userservice.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequestDto {
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
}