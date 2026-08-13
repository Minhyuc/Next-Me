package com.nm.loginservice.Service;

import com.nm.loginservice.Dto.LoginRequestDto;
import com.nm.loginservice.Dto.LoginResponseDto;
import com.nm.loginservice.Dto.UserSyncRequestDto;
import com.nm.loginservice.Entity.User;
import com.nm.loginservice.Repository.LoginRepository;
import com.nm.loginservice.Security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LoginService {

    private final LoginRepository repository;
    private final EmailverificationService emailVerificationService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    public LoginService(LoginRepository repository,
                        EmailverificationService emailVerificationService,
                        JwtUtil jwtUtil,
                        PasswordEncoder passwordEncoder,
                        RestTemplate restTemplate) {
        this.repository = repository;
        this.emailVerificationService = emailVerificationService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
    }

    // 회원가입
    public String register(LoginRequestDto dto) {

        if (repository.findByUsername(dto.getUsername()).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }

        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            return "이미 존재하는 이메일입니다.";
        }

        if (!emailVerificationService.isVerified(dto.getEmail())) {
            return "이메일 인증이 완료되지 않았습니다.";
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        User user = new User(dto.getUsername(), encodedPassword, dto.getName(), dto.getEmail());
        repository.save(user);

        return "회원가입 성공";
    }

    // 로그인
    public LoginResponseDto login(LoginRequestDto dto) {

        User user = repository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 틀렸습니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 틀렸습니다.");
        }

        // JWT 생성: userId만 사용
        String token = jwtUtil.createAccessToken(user.getId());

        // user-service 동기화: userId만 전달
        try {
            UserSyncRequestDto syncDto = new UserSyncRequestDto();
            syncDto.setUserId(user.getId());

            restTemplate.postForObject(
                    "http://localhost:8082/users/sync",
                    syncDto,
                    String.class
            );
        } catch (Exception e) {
            System.out.println("user-service sync failed: " + e.getMessage());
        }

        return new LoginResponseDto(
                user.getId(),
                token,
                user.getUsername(),
                user.getName(),
                user.getEmail()
        );
    }
}