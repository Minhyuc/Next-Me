package com.nm.usercareservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 사용자 프로필 기반 세안·스킨케어 성분 추천을 담당하는 마이크로서비스다. */
@SpringBootApplication
public class UserCareServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserCareServiceApplication.class, args);
    }
}
