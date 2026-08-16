package com.kuky.backend;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full-stack tests against PostgreSQL. They do not use the local {@code kuky_dev} database.
 *
 * <p>On GitHub Actions a throwaway Postgres 18 service is started by {@code ci.yml} and
 * {@code application-test.yaml} points at {@code localhost:5432/kuky_test}. Locally these
 * tests are skipped ({@code GITHUB_ACTIONS} is unset); unit tests still run.
 *
 * <p>The skip uses {@code @ExtendWith} rather than {@code @EnabledIfEnvironmentVariable}
 * because the latter is not {@code @Inherited} and would not apply to subclasses.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(AbstractIntegrationTest.GitHubActionsOnly.class)
public abstract class AbstractIntegrationTest {

    static final class GitHubActionsOnly implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            if ("true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
                return ConditionEvaluationResult.enabled("Running on GitHub Actions");
            }
            return ConditionEvaluationResult.disabled(
                    "Integration tests run on GitHub Actions against a throwaway Postgres service");
        }
    }
}
