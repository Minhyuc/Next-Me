package com.nm.routineservice.Exception;

/** JWT에서 현재 사용자 ID를 확인하지 못했을 때 발생하는 인증 예외다. */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
