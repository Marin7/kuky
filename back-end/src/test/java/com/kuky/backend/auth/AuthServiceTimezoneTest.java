package com.kuky.backend.auth;

import com.kuky.backend.auth.dto.UpdateTimezoneRequest;
import com.kuky.backend.auth.dto.UserResponse;
import com.kuky.backend.auth.exception.InvalidTimezoneException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.EmailActivationTokenRepository;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.auth.service.AuthService;
import com.kuky.backend.auth.service.EmailService;
import com.kuky.backend.config.JwtConfig;
import com.kuky.backend.presentations.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pure-logic tests for the timezone sync/override rules — no Spring context, no database. */
class AuthServiceTimezoneTest {

    private UserRepository userRepository;
    private AuthService authService;

    private final UUID userId = UUID.randomUUID();
    private final String email = "student@example.com";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = new AuthService(
                userRepository,
                mock(BCryptPasswordEncoder.class),
                mock(JwtConfig.class),
                mock(ImageService.class),
                mock(EmailActivationTokenRepository.class),
                mock(EmailService.class));
    }

    private User existingUser(String timezone, boolean manual) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setTimezone(timezone);
        user.setTimezoneManual(manual);
        return user;
    }

    @Test
    void autoSync_overwritesTimezone_whenNotManual() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingUser(null, false)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser("Europe/Bucharest", false)));

        UserResponse response = authService.updateTimezone(email, new UpdateTimezoneRequest("Europe/Bucharest", false));

        verify(userRepository).updateTimezone(userId, "Europe/Bucharest", false);
        assertThat(response.timezone()).isEqualTo("Europe/Bucharest");
        assertThat(response.timezoneIsManual()).isFalse();
    }

    @Test
    void manualOverride_persistsAndIsReturned() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingUser("Europe/Bucharest", false)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser("America/New_York", true)));

        UserResponse response = authService.updateTimezone(email, new UpdateTimezoneRequest("America/New_York", true));

        verify(userRepository).updateTimezone(userId, "America/New_York", true);
        assertThat(response.timezone()).isEqualTo("America/New_York");
        assertThat(response.timezoneIsManual()).isTrue();
    }

    @Test
    void invalidZoneId_isRejected_withoutPersisting() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingUser(null, false)));

        assertThatThrownBy(() ->
                authService.updateTimezone(email, new UpdateTimezoneRequest("Not/AZone", false)))
                .isInstanceOf(InvalidTimezoneException.class);

        verify(userRepository, never()).updateTimezone(any(), any(), eq(false));
        verify(userRepository, never()).updateTimezone(any(), any(), eq(true));
    }
}
