package com.kuky.backend;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots a throwaway PostgreSQL 18 container for {@code @SpringBootTest} classes so they do not
 * use the developer's {@code kuky_dev} database. Disabled when Docker is missing so local
 * {@code ./gradlew test} still runs unit tests; GitHub Actions has Docker and runs these.
 *
 * <p>{@code @ServiceConnection} is not used: Spring Boot does not apply it to {@code @Container}
 * fields declared on a superclass, so tests would keep {@code application-test.yaml}'s
 * {@code localhost:5432} URL. {@code @DynamicPropertySource} on this class is inherited.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(AbstractIntegrationTest.SkipWithoutDocker.class)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
                    .withDatabaseName("kuky_test")
                    .withUsername("kuky")
                    .withPassword("kuky");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    static final class SkipWithoutDocker implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
            return ConditionEvaluationResult.disabled(
                    "Docker is not available; integration tests run in CI");
        }
    }
}
