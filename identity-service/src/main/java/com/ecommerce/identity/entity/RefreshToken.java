package com.ecommerce.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity RefreshToken - Bảng refresh_tokens trong identity_db.
 * Lưu trữ refresh token để cấp lại access token khi hết hạn.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
