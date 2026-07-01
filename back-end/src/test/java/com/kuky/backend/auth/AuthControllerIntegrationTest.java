package com.kuky.backend.auth;

import com.kuky.backend.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards against regressing to a hardcoded role in the auth-token cookie issued at
 * registration — it must reflect the account's real (default USER) role, not a
 * stale assumption that every new account is a STUDENT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtConfig jwtConfig;

    private MockMvc mockMvc;
    private String testEmail;

    @AfterEach
    void tearDown() {
        if (testEmail != null) {
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", testEmail);
        }
    }

    @Test
    void register_issuesAuthCookieWithUserRole_notStudent() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        testEmail = "register-role-" + UUID.randomUUID() + "@example.com";
        String body = "{\"email\":\"" + testEmail + "\",\"password\":\"TestPassword123!\",\"gdprConsent\":true}";

        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("auth-token");
        assertThat(authCookie).isNotNull();
        assertThat(jwtConfig.extractRole(authCookie.getValue())).isEqualTo("USER");

        String dbRole = jdbcTemplate.queryForObject("SELECT role FROM users WHERE email = ?", String.class, testEmail);
        assertThat(dbRole).isEqualTo("USER");
    }
}
