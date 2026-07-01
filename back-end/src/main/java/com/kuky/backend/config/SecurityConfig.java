package com.kuky.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtCookieAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtCookieAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .accessDeniedHandler(jsonAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/schedule").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/resources/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/purchases").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/v1/learning/**").hasAnyRole("STUDENT", "ADMIN")
                        .anyRequest().authenticated()
                )
                .build();
    }

    /** Unauthenticated request to a protected endpoint → 401 with a JSON error body. */
    private AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) ->
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHENTICATED", "Debes iniciar sesión.");
    }

    /** Authenticated but unauthorized (non-admin hitting /admin/**) → 403 ACCESS_DENIED. */
    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "ACCESS_DENIED", "No tienes permiso para acceder a esta sección.");
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
