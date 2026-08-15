package com.nm.usercareservice.Controller;

import com.nm.usercareservice.Dto.ErrorResponseDto;
import com.nm.usercareservice.Exception.AuthenticationRequiredException;
import com.nm.usercareservice.Exception.RecommendationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/** 예외를 프론트가 처리하기 쉬운 message 한 필드 형식으로 변환한다. */
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorized(
            AuthenticationRequiredException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(RecommendationNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            RecommendationNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceError(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponseDto(exception.getMessage()));
    }
}
