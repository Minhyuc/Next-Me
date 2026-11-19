package com.nm.loginservice.Controller;

import com.nm.loginservice.Dto.EmailSendRequestDto;
import com.nm.loginservice.Dto.EmailverifyRequestDto;
import com.nm.loginservice.Dto.LoginRequestDto;
import com.nm.loginservice.Dto.LoginResponseDto;
import com.nm.loginservice.Service.EmailverificationService;
import com.nm.loginservice.Service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService service;
    private final EmailverificationService emailVerificationService;

    public LoginController(LoginService service, EmailverificationService emailVerificationService) {
        this.service = service;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(service.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @PostMapping("/email/send")
    public ResponseEntity<String> send(@RequestBody EmailSendRequestDto dto) {
        String result = emailVerificationService.sendVerificationCode(dto.getEmail());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/email/verify")
    public ResponseEntity<String> verify(@RequestBody EmailverifyRequestDto dto) {
        String result = emailVerificationService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(result);
    }
}