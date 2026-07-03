package com.kuky.backend.auth.service;

import com.kuky.backend.auth.dto.AuthResponse;
import com.kuky.backend.auth.dto.LoginRequest;
import com.kuky.backend.auth.dto.RegisterRequest;
import com.kuky.backend.auth.dto.UpdateProfileRequest;
import com.kuky.backend.auth.dto.UpdateTimezoneRequest;
import com.kuky.backend.auth.dto.UserResponse;
import com.kuky.backend.auth.exception.AccountNotActivatedException;
import com.kuky.backend.auth.exception.AuthException;
import com.kuky.backend.auth.exception.DuplicateEmailException;
import com.kuky.backend.auth.exception.DuplicateUsernameException;
import com.kuky.backend.auth.exception.InvalidTimezoneException;
import com.kuky.backend.auth.exception.InvalidTokenException;
import com.kuky.backend.auth.model.EmailActivationToken;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.EmailActivationTokenRepository;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.JwtConfig;
import com.kuky.backend.presentations.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final ImageService imageService;
    private final EmailActivationTokenRepository activationTokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtConfig jwtConfig,
                       ImageService imageService,
                       EmailActivationTokenRepository activationTokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.imageService = imageService;
        this.activationTokenRepository = activationTokenRepository;
        this.emailService = emailService;
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
        user.setStatus("PENDING");
        user = userRepository.save(user);

        issueActivationEmail(user);

        return new AuthResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    public UserResponse login(LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("Correo electrónico o contraseña incorrectos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Correo electrónico o contraseña incorrectos.");
        }

        if ("PENDING".equals(user.getStatus())) {
            throw new AccountNotActivatedException(
                    "Por favor, activa tu cuenta desde el correo que te enviamos.");
        }

        return toResponse(user);
    }

    public UserResponse activate(String tokenValue) {
        EmailActivationToken token = activationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException(
                        "El enlace de activación no es válido o ha expirado. Solicita uno nuevo."));

        if (token.isUsed()) {
            throw new InvalidTokenException("Este enlace de activación ya fue utilizado.");
        }

        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new InvalidTokenException(
                    "El enlace de activación ha expirado. Solicita uno nuevo.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));

        user.setStatus("ACTIVE");
        userRepository.save(user);

        token.setUsed(true);
        activationTokenRepository.save(token);

        return toResponse(user);
    }

    public void resendActivation(String email) {
        userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .filter(u -> "PENDING".equals(u.getStatus()))
                .ifPresent(this::issueActivationEmail);
        // Silent — never reveal whether email exists or is pending
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT));
    }

    public Optional<UserResponse> findUserResponse(String email) {
        return findByEmail(email).map(this::toResponse);
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));

        String firstName = normalize(request.firstName());
        String lastName  = normalize(request.lastName());
        String username  = normalize(request.username());

        if (username != null) {
            if (username.length() < 3) {
                throw new IllegalArgumentException(
                        "El nombre de usuario debe tener al menos 3 caracteres.");
            }
            if (!username.matches("[a-zA-Z0-9_-]+")) {
                throw new IllegalArgumentException(
                        "El nombre de usuario solo puede contener letras, números, guiones y guiones bajos.");
            }
            if (userRepository.existsByUsernameIgnoreCase(username, user.getId())) {
                throw new DuplicateUsernameException("Este nombre de usuario ya está en uso.");
            }
        }

        userRepository.updateProfile(user.getId(), firstName, lastName, username);

        return userRepository.findById(user.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));
    }

    public UserResponse updateTimezone(String email, UpdateTimezoneRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));

        try {
            ZoneId.of(request.zone());
        } catch (DateTimeException e) {
            throw new InvalidTimezoneException("La zona horaria indicada no es válida.");
        }

        userRepository.updateTimezone(user.getId(), request.zone(), request.manual());

        return userRepository.findById(user.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));
    }

    public UUID uploadAvatar(String email, MultipartFile file) {
        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AuthException("Usuario no encontrado."));

        ImageService.UploadResult result = imageService.store(file);
        userRepository.updateAvatar(user.getId(), result.id());
        return result.id();
    }

    UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole(),
                u.getFirstName(), u.getLastName(), u.getUsername(),
                u.getAvatarImageId(), u.getStatus(),
                u.getTimezone(), u.isTimezoneManual(),
                u.isExtendedClassEligible());
    }

    private void issueActivationEmail(User user) {
        activationTokenRepository.invalidateAllForUser(user.getId());

        EmailActivationToken token = new EmailActivationToken();
        token.setUserId(user.getId());
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        token.setUsed(false);
        activationTokenRepository.save(token);

        try {
            emailService.sendActivationEmail(user.getEmail(), token.getToken());
        } catch (Exception e) {
            log.warn("EmailService — activation URL for {}: ?activateToken={}",
                    user.getEmail(), token.getToken());
        }
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
