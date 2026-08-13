package com.nm.aichatservice.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 개발 중 프론트엔드가 다른 주소에서 API를 호출할 수 있도록 CORS를 허용한다. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 모든 API 경로에서 Origin, HTTP 메서드, 헤더를 허용한다.
        // 운영 환경에서는 allowedOrigins를 실제 프론트 주소로 제한하는 것이 안전하다.
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
