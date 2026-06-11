package com.kuky.backend.auth.controller;

import com.kuky.backend.auth.dto.*;
import com.kuky.backend.auth.exception.AccountNotActivatedException;
import com.kuky.backend.auth.exception.RateLimitException;
import com.kuky.backend.auth.service.AuthService;
import com.kuky.backend.auth.service.LoginRateLimiter;
import com.kuky.backend.auth.service.PasswordResetService;
import com.kuky.backend.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final LoginRateLimiter loginRateLimiter;
    private final JwtConfig jwtConfig;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          LoginRateLimiter loginRateLimiter,
                          JwtConfig jwtConfig) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.loginRateLimiter = loginRateLimiter;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResponse body = authService.register(request);
        String token = jwtConfig.generateToken(body.id(), body.email(), "STUDENT");
        setAuthCookie(response, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse response) {
        String ip = getClientIp(httpRequest);
        if (!loginRateLimiter.tryConsume(ip)) {
            throw new RateLimitException("Demasiados intentos. Por favor, espera un momento e inténtalo de nuevo.");
        }
        UserResponse body = authService.login(request);
        String token = jwtConfig.generateToken(body.id(), body.email(), body.role());
        setAuthCookie(response, token);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("auth-token", "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal String email) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return authService.findUserResponse(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal String email,
            @RequestParam("file") MultipartFile file) {
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID avatarImageId = authService.uploadAvatar(email, file);
        return ResponseEntity.ok(Map.of("avatarImageId", avatarImageId.toString()));
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> activate(
            @Valid @RequestBody ActivateRequest request,
            HttpServletResponse response) {
        UserResponse user = authService.activate(request.token());
        String token = jwtConfig.generateToken(user.id(), user.email(), user.role());
        setAuthCookie(response, token);
        return ResponseEntity.ok(Map.of("message", "¡Cuenta activada! Ya puedes usar Kuky."));
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<Map<String, String>> resendActivation(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendActivation(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Si tu cuenta está pendiente de activación, recibirás un nuevo enlace."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado, recibirás un enlace de recuperación en breve."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletResponse response) {
        UserResponse user = passwordResetService.consumeToken(request.token(), request.newPassword());
        String token = jwtConfig.generateToken(user.id(), user.email(), user.role());
        setAuthCookie(response, token);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("auth-token", token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtConfig.getExpirySeconds())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String xfwd = request.getHeader("X-Forwarded-For");
        if (xfwd != null && !xfwd.isBlank()) {
            return xfwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
