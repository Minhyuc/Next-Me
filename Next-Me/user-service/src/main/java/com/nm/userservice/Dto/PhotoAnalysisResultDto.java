package com.nm.userservice.Dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoAnalysisResultDto {
    private String skinType;
    private String personalColor;
    private String message;
    private Integer confidence; // 0~100
}