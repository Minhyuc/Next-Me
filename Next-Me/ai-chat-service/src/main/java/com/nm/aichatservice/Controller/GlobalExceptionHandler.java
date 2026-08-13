package com.nm.aichatservice.Controller;

import com.nm.aichatservice.Dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 서비스에서 발생한 예외를 프론트가 처리하기 쉬운 JSON 오류 응답으로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    /** 잘못된 대화방 ID나 인증 누락 등 클라이언트 요청 오류를 400으로 반환한다. */
    public ResponseEntity<ErrorResponseDto> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /** @Valid 검증 실패 중 첫 번째 메시지를 400 응답에 담는다. */
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(new ErrorResponseDto(message));
    }

    @ExceptionHandler(IllegalStateException.class)
    /** OpenAI 또는 외부 서비스 처리 실패를 502로 반환한다. */
    public ResponseEntity<ErrorResponseDto> handleServerError(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponseDto(exception.getMessage()));
    }
}
