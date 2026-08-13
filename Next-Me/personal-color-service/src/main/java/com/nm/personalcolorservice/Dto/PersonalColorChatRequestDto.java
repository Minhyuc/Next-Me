package com.nm.personalcolorservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 퍼스널컬러 분석 AI와 대화할 때 보내는 요청이다. */
public class PersonalColorChatRequestDto {
    private Long conversationId; // null이면 분석 시작, 값이 있으면 해당 분석 대화를 이어간다.

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 1000, message = "메시지는 1000자 이하로 입력해주세요.")
    private String message; // 피부·머리·눈 색상 등에 관한 사용자 답변
}
