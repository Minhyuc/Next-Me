package com.nm.loginservice.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Emailverification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String codeSalt;

    private String codeHash;

    private boolean verified;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;
}