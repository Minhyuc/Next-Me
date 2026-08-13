package com.nm.personalcolorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 퍼스널컬러 분석과 옷·화장 추천 마이크로서비스의 실행 시작점이다. */
@SpringBootApplication
public class PersonalColorServiceApplication {

    public static void main(String[] args) {
        // Spring 컨테이너와 내장 웹 서버를 시작한다.
        SpringApplication.run(PersonalColorServiceApplication.class, args);
    }
}
