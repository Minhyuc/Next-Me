package com.nm.usercareservice.Exception;

/** 아직 생성된 사용자 피부 관리 추천이 없을 때 발생한다. */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException(String message) {
        super(message);
    }
}
