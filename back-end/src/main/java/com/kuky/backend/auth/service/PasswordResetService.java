package com.kuky.backend.auth.service;

import com.kuky.backend.auth.dto.UserResponse;
import com.kuky.backend.auth.exception.AuthException;
import com.kuky.backend.auth.exception.InvalidTokenException;
import com.kuky.backend.auth.model.PasswordResetToken;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.PasswordResetTokenRepository;
import com.kuky.backend.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT)).ifPresent(user -> {
            invalidateAllTokensForUser(user.getId());

            PasswordResetToken prt = new PasswordResetToken();
            prt.setUserId(user.getId());
            prt.setToken(UUID.randomUUID().toString());
            prt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            prt.setUsed(false);
            tokenRepository.save(prt);

            emailService.sendPasswordResetEmail(user.getEmail(), prt.getToken());
        });
    }

    public UserResponse consumeToken(String tokenValue, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException(
                        "El enlace de recuperación no es válido o ha expirado. Solicita uno nuevo."));

        if (prt.isUsed()) {
            throw new InvalidTokenException(
                    "El enlace de recuperación no es válido o ha expirado. Solicita uno nuevo.");
        }

        if (Instant.now().isAfter(prt.getExpiresAt())) {
            throw new InvalidTokenException(
                    "El enlace de recuperación no es válido o ha expirado. Solicita uno nuevo.");
        }

        User user = userRepository.findById(prt.getUserId())
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);
        invalidateAllTokensForUser(user.getId());

        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    private void invalidateAllTokensForUser(UUID userId) {
        List<PasswordResetToken> tokens = tokenRepository.findAllByUserIdAndUsedFalse(userId);
        tokens.forEach(t -> t.setUsed(true));
        tokenRepository.saveAll(tokens);
    }
}
