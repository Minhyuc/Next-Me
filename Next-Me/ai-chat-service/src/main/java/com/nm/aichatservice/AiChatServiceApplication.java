package com.nm.aichatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** AI 피부·운동·루틴 상담 마이크로서비스의 실행 시작점이다. */
@SpringBootApplication
public class AiChatServiceApplication {

    public static void main(String[] args) {
        // Spring 컨테이너를 만들고 내장 웹 서버를 시작한다.
        SpringApplication.run(AiChatServiceApplication.class, args);
    }
}
