package com.nm.aichatservice.Processor;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** 애플리케이션 설정을 읽기 전에 프로젝트의 .env 값을 Spring 환경 변수로 등록한다. */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // .env가 없더라도 즉시 실패하지 않아 운영 서버의 시스템 환경 변수도 사용할 수 있다.
        Dotenv dotenv = Dotenv.configure()
                .directory(findDotenvDirectory())
                .filename(".env")
                .ignoreIfMissing()
                .load();

        Map<String, Object> properties = new HashMap<>();
        dotenv.entries().forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
        // 가비아의 실제 서버 환경 변수가 로컬 개발용 .env보다 우선한다.
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", properties));
    }

    private String findDotenvDirectory() {
        // IntelliJ 실행 위치가 루트 또는 하위 모듈이어도 .env를 찾도록 후보 경로를 순서대로 검사한다.
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path parent = workingDirectory.getParent();
        Path[] candidates = {
                workingDirectory,
                workingDirectory.resolve("Next-Me"),
                parent,
                parent == null ? null : parent.resolve("Next-Me")
        };

        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate.resolve(".env"))) {
                return candidate.toString();
            }
        }
        return workingDirectory.toString();
    }

    @Override
    public int getOrder() {
        // application.yml의 ${변수명} 치환 전에 .env가 등록되도록 가장 먼저 실행한다.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
