package com.kuky.backend.auth.service;

import com.kuky.backend.auth.dto.AuthResponse;
import com.kuky.backend.auth.dto.LoginRequest;
import com.kuky.backend.auth.dto.RegisterRequest;
import com.kuky.backend.auth.dto.UserResponse;
import com.kuky.backend.auth.exception.AuthException;
import com.kuky.backend.auth.exception.DuplicateEmailException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.JwtConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("Este correo electrónico ya está registrado.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGdprConsent(true);
        user.setStatus("ACTIVE");

        user = userRepository.save(user);
        return new AuthResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    public UserResponse login(LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("Correo electrónico o contraseña incorrectos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Correo electrónico o contraseña incorrectos.");
        }

        return new UserResponse(user.getId(), user.getEmail());
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT));
    }
}
