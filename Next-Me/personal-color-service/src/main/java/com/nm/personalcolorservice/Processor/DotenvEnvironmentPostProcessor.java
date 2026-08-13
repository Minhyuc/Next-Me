package com.nm.personalcolorservice.Processor;

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

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    // Spring 설정을 읽기 전에 .env 값을 가장 높은 우선순위 PropertySource로 등록한다.

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
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
        // IDE 실행 위치가 루트 또는 하위 모듈이어도 공통 .env를 찾도록 후보를 순회한다.
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
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
