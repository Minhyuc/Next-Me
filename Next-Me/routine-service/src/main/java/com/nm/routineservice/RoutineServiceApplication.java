package com.nm.routineservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** AI 루틴 생성·완료 관리와 정기 초기화를 담당하는 마이크로서비스의 실행 시작점이다. */
@EnableScheduling // 만료 항목과 날짜 경계를 주기적으로 확인하는 스케줄러를 활성화한다.
@SpringBootApplication
public class RoutineServiceApplication {

    public static void main(String[] args) {
        // Spring 컨테이너와 내장 웹 서버를 시작한다.
        SpringApplication.run(RoutineServiceApplication.class, args);
    }
}
