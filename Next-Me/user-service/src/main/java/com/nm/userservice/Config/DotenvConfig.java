package com.nm.userservice.Config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("/Users/yunminhyuck/Documents/Codex/Next-Me/Next-Me")
                .filename(".env")
                .ignoreIfMissing()
                .load();
    }
}