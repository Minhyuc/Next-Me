package com.nm.userservice.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingNextResponseDto {
    private boolean completed;
    private String detectedSkinType;
    private String detectedPersonalColor;
    private List<OnboardingQuestionDto> nextQuestions;
    private UserResponseDto profile;

    private Boolean photoRetryRequired;
    private String photoRetryMessage;
    private String analysisMessage;

    //사진 점수
    private Integer skinTypeConfidence;
    private Integer personalColorConfidence;
}