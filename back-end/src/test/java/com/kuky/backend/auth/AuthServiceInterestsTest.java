package com.kuky.backend.auth;

import com.kuky.backend.auth.dto.UpdateInterestsRequest;
import com.kuky.backend.auth.dto.UserResponse;
import com.kuky.backend.auth.exception.InterestsAccessDeniedException;
import com.kuky.backend.auth.exception.InvalidInterestsException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class AuthServiceInterestsTest {

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

    private User student() {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole("STUDENT");
        return user;
    }

    private User withNote(User user, String note) {
        user.setInterestsNote(note);
        return user;
    }

    @Test
    void updateInterests_persistsCodesAndNote_forStudent() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student()));
        User refreshed = withNote(student(), "Me gusta el flamenco");
        when(userRepository.findById(userId)).thenReturn(Optional.of(refreshed));
        when(userRepository.findInterestCodesByUserId(userId))
                .thenReturn(List.of("FOOD", "MUSIC", "TRAVEL"));

        UserResponse response = authService.updateInterests(
                email,
                new UpdateInterestsRequest(List.of("travel", "MUSIC", "FOOD"), "Me gusta el flamenco"));

        verify(userRepository).replaceInterests(userId, List.of("TRAVEL", "MUSIC", "FOOD"));
        verify(userRepository).updateInterestsNote(userId, "Me gusta el flamenco");
        assertThat(response.interests()).containsExactly("FOOD", "MUSIC", "TRAVEL");
        assertThat(response.interestsNote()).isEqualTo("Me gusta el flamenco");
    }

    @Test
    void updateInterests_rejectsUserRole() {
        User user = student();
        user.setRole("USER");
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.updateInterests(email, new UpdateInterestsRequest(List.of("MUSIC"), null)))
                .isInstanceOf(InterestsAccessDeniedException.class);

        verify(userRepository, never()).replaceInterests(any(), any());
    }

    @Test
    void updateInterests_rejectsUnknownCode() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student()));

        assertThatThrownBy(() ->
                authService.updateInterests(email, new UpdateInterestsRequest(List.of("GAMING"), null)))
                .isInstanceOf(InvalidInterestsException.class)
                .satisfies(ex -> assertThat(((InvalidInterestsException) ex).getErrorCode())
                        .isEqualTo("INVALID_INTEREST"));

        verify(userRepository, never()).replaceInterests(any(), any());
    }

    @Test
    void updateInterests_rejectsMoreThanTen() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student()));
        List<String> eleven = List.of(
                "TRAVEL", "MUSIC", "SPORTS", "FOOD", "CINEMA", "READING",
                "TECHNOLOGY", "NATURE", "ART", "WORK", "FAMILY");

        assertThatThrownBy(() ->
                authService.updateInterests(email, new UpdateInterestsRequest(eleven, null)))
                .isInstanceOf(InvalidInterestsException.class)
                .satisfies(ex -> assertThat(((InvalidInterestsException) ex).getErrorCode())
                        .isEqualTo("TOO_MANY_INTERESTS"));
    }

    @Test
    void updateInterests_clearsWhenEmpty() {
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(student()));
        when(userRepository.findInterestCodesByUserId(userId)).thenReturn(List.of());

        UserResponse response = authService.updateInterests(
                email, new UpdateInterestsRequest(List.of(), "  "));

        verify(userRepository).replaceInterests(eq(userId), eq(List.of()));
        verify(userRepository).updateInterestsNote(eq(userId), isNull());
        assertThat(response.interests()).isEmpty();
        assertThat(response.interestsNote()).isNull();
    }

    @Test
    void toResponse_filtersUnknownStoredCodes() {
        User user = student();
        user.setInterestsNote("hola");
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(userRepository.findInterestCodesByUserId(userId))
                .thenReturn(List.of("MUSIC", "RETIRED_CODE", "FOOD"));

        UserResponse again = authService.findUserResponse(email).orElseThrow();
        assertThat(again.interests()).containsExactly("MUSIC", "FOOD");
        assertThat(again.interestsNote()).isEqualTo("hola");
    }
}
