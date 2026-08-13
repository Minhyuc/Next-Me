package com.nm.userservice.Dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingQuestionDto {
    private String field;
    private String question;
    private String type;
    private Boolean required;
    private List<String> options;
}
