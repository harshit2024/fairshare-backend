package com.fairShare.fairshare_backend.auth.service;

import com.fairShare.fairshare_backend.auth.dto.*;
import com.fairShare.fairshare_backend.auth.entity.OneTimeToken;
import com.fairShare.fairshare_backend.auth.entity.RefreshToken;
import com.fairShare.fairshare_backend.auth.entity.TokenPurpose;
import com.fairShare.fairshare_backend.auth.repository.OneTimeTokenRepository;
import com.fairShare.fairshare_backend.auth.repository.RefreshTokenRepository;
import com.fairShare.fairshare_backend.security.JwtService;
import com.fairShare.fairshare_backend.users.User;
import com.fairShare.fairshare_backend.users.UserRepository;
import com.fairShare.fairshare_backend.users.UserStatus;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OneTimeTokenRepository oneTimeTokenRepository ;
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository, OneTimeTokenRepository oneTimeTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository =
                refreshTokenRepository;
        this.oneTimeTokenRepository = oneTimeTokenRepository;
    }

    public LoginResponse login(LoginRequest request) {

        String email =
                request.email().trim().toLowerCase();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "Account is not active"
            );
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {

            user.setFailedLoginAttempts(
                    user.getFailedLoginAttempts() + 1
            );

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                generateRefreshToken();

        saveRefreshToken(
                user,
                refreshToken
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration()
        );
    }

    private String generateRefreshToken() {

        byte[] bytes = new byte[64];

        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }


    private void saveRefreshToken(User user, String rawToken) {
        String hash = sha256Hex(rawToken);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(LocalDateTime.now().plusDays(30));

        refreshTokenRepository.save(token);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        String hash = sha256Hex(rawRefreshToken);

        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token ->
                        token.setRevokedAt(LocalDateTime.now()));
    }

    private String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", e
            );
        }
    }
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password())
        );

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Handles concurrent registration attempts for the same email.
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEmailVerified()
        );
    }

    public LoginResponse refresh(RefreshRequest request) {
        String rawToken = request.refreshToken();

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String hash = sha256Hex(rawToken);

        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid refresh token")
                );

        LocalDateTime now = LocalDateTime.now();

        if (existing.getRevokedAt() != null ||
                !existing.getExpiresAt().isAfter(now)) {
            throw new BadCredentialsException(
                    "Refresh token is expired or revoked"
            );
        }

        User user = existing.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account is not active");
        }

        // Rotate the refresh token: revoke the old one and save a new one.
        String newRawToken = generateRefreshToken();

        existing.setRevokedAt(now);
        existing.setLastUsedAt(now);

        RefreshToken replacement = new RefreshToken();
        replacement.setUser(user);
        replacement.setTokenHash(sha256Hex(newRawToken));
        replacement.setExpiresAt(now.plusDays(30));

        refreshTokenRepository.save(replacement);

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                accessToken,
                newRawToken,
                jwtService.getAccessTokenExpiration()
        );
    }


//    public void forgotPassword(String email) {
//        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
//
//        // Return the same API response whether the account exists or not.
//        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
//            issueOneTimeToken(
//                    user,
//                    TokenPurpose.PASSWORD_RESET,
//                    LocalDateTime.now().plusMinutes(30)
//            );
//        });
//    }

    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        if (newPassword == null || newPassword.length() < 8
                || newPassword.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 72 characters"
            );
        }


        OneTimeToken token = oneTimeTokenRepository
                .findByTokenHashAndPurpose(
                        sha256Hex(rawToken),
                        TokenPurpose.PASSWORD_RESET
                )
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset token"
                ));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        token.setUsedAt(LocalDateTime.now());

        // Persist the password and consumed token in the same transaction.
        userRepository.save(user);
        oneTimeTokenRepository.save(token);
    }

    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid or expired verification token"
            );
        }

        OneTimeToken token = oneTimeTokenRepository
                .findByTokenHashAndPurpose(
                        sha256Hex(rawToken),
                        TokenPurpose.EMAIL_VERIFICATION
                )
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired verification token"
                ));

        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());

        userRepository.save(user);
        oneTimeTokenRepository.save(token);
    }

//    public void resendVerification(String email) {
//        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
//
//        // Do not reveal whether an account exists or is already verified.
//        userRepository.findByEmail(normalizedEmail)
//                .filter(user -> !user.isEmailVerified())
//                .ifPresent(user -> issueOneTimeToken(
//                        user,
//                        TokenPurpose.EMAIL_VERIFICATION,
//                        LocalDateTime.now().plusHours(24)
//                ));
//    }

//    private void issueOneTimeToken(
//            User user,
//            TokenPurpose purpose,
//            LocalDateTime expiresAt
//    ) {
//        // Invalidate previous unused tokens of this purpose.
//        List<OneTimeToken> previousTokens =
//                oneTimeTokenRepository
//                        .findAllByUser_IdAndPurposeAndUsedAtIsNull(
//                                user.getId(), purpose
//                        );
//
//        LocalDateTime now = LocalDateTime.now();
//        previousTokens.forEach(t -> t.setUsedAt(now));
//        oneTimeTokenRepository.saveAll(previousTokens);
//
//        byte[] bytes = new byte[32];
//        new SecureRandom().nextBytes(bytes);
//
//        String rawToken = Base64.getUrlEncoder()
//                .withoutPadding()
//                .encodeToString(bytes);
//
//        OneTimeToken token = new OneTimeToken();
//        token.setUser(user);
//        token.setTokenHash(sha256Hex(rawToken));
//        token.setPurpose(purpose);
//        token.setExpiresAt(expiresAt);
//
//        oneTimeTokenRepository.save(token);
//
//        // Send the raw token only by email; the database stores its hash.
//        if (purpose == TokenPurpose.PASSWORD_RESET) {
//            authEmailService.sendPasswordReset(user.getEmail(), rawToken);
//        } else {
//            authEmailService.sendEmailVerification(user.getEmail(), rawToken);
//        }
//    }
}
