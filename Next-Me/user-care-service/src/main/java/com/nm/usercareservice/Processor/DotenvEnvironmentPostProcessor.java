package com.nm.usercareservice.Processor;

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

/** 로컬에서는 공통 .env를 사용하고 배포 서버에서는 실제 환경 변수를 우선한다. */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

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
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", properties));
    }

    private String findDotenvDirectory() {
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
