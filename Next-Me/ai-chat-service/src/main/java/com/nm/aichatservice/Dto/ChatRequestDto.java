package com.nm.aichatservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 프론트가 AI에게 메시지를 보낼 때 사용하는 요청 값이다. */
public class ChatRequestDto {

    private Long conversationId; // null이면 새 대화, 값이 있으면 해당 대화를 이어간다.

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 2000, message = "메시지는 2,000자 이하로 입력해주세요.")
    private String message; // 사용자가 입력한 질문(최대 2,000자)
}
