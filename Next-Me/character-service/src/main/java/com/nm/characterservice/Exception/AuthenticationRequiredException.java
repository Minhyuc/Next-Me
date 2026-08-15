package com.nm.characterservice.Exception;

/** JWT가 없거나 유효하지 않아 현재 사용자를 확인할 수 없을 때 발생한다. */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
