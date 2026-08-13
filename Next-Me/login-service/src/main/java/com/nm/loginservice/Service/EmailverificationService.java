package com.nm.loginservice.Service;

import com.nm.loginservice.Entity.Emailverification;
import com.nm.loginservice.Repository.EmailverificationRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailverificationService {

    private final EmailverificationRepository repository;
    private final JavaMailSender mailSender;

    public EmailverificationService(EmailverificationRepository repository, JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    @Transactional
    public String sendVerificationCode(String email) {
        if (email == null || email.isBlank()) {
            return "이메일을 입력해주세요.";
        }

        email = email.trim();

        String code = generateCode();
        String salt = UUID.randomUUID().toString();
        String hash = hashCode(salt, code);

        Emailverification verification = repository.findByEmail(email)
                .orElseGet(Emailverification::new);

        verification.setEmail(email);
        verification.setCodeSalt(salt);
        verification.setCodeHash(hash);
        verification.setVerified(false);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verification.setVerifiedAt(null);

        repository.save(verification);

        sendMail(email, code);

        return "인증번호가 이메일로 발송되었습니다.";
    }

    @Transactional
    public String verifyCode(String email, String code) {
        Optional<Emailverification> optional = repository.findByEmail(email);

        if (optional.isEmpty()) {
            return "인증 요청이 존재하지 않습니다.";
        }

        Emailverification verification = optional.get();

        if (verification.isVerified()) {
            return "이미 인증된 이메일입니다.";
        }

        if (verification.getExpiresAt() == null || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "인증번호가 만료되었습니다.";
        }

        String inputHash = hashCode(verification.getCodeSalt(), code);

        if (!verification.getCodeHash().equals(inputHash)) {
            return "인증번호가 일치하지 않습니다.";
        }

        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        repository.save(verification);

        return "이메일 인증이 완료되었습니다.";
    }

    public boolean isVerified(String email) {
        return repository.findByEmail(email)
                .map(Emailverification::isVerified)
                .orElse(false);
    }

    private void sendMail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("minhyuckyun@gmail.com");
            message.setTo(email);
            message.setSubject("[Next-Me] 이메일 인증번호");
            message.setText("인증번호는 다음과 같습니다: " + code + "\n\n10분 내에 입력해주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("메일 발송 실패", e);
        }
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    private String hashCode(String salt, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = salt + ":" + code;
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("인증번호 해시 생성 실패", e);
        }
    }
}