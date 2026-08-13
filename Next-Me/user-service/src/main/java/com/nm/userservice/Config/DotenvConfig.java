package com.nm.userservice.Config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class DotenvConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory(findDotenvDirectory())
                .filename(".env")
                .ignoreIfMissing()
                .load();
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
}
