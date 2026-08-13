package com.nm.loginservice.Security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/** 로그인 성공 시 다른 MSA 서비스가 공통으로 검증할 JWT를 발급한다. */
@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secretKey) {
        // 모든 서비스가 같은 CREATE_JWT 값을 사용해야 동일한 토큰을 검증할 수 있다.
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /** 사용자 ID를 subject와 claim에 넣은 만료 시각 없는 액세스 토큰을 만든다. */
    public String createAccessToken(Long userId) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .setIssuedAt(now)
                // MVP 정책상 exp를 넣지 않아 사용자가 직접 로그아웃하기 전까지 토큰이 유지된다.
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
