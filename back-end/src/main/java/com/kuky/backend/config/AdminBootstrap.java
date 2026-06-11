package com.kuky.backend.config;

import com.kuky.backend.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Promotes the configured teacher account ({@code app.scheduling.teacher-email}) to ADMIN
 * on startup. Idempotent: a no-op if the account is missing or already ADMIN. The role lands
 * in the JWT on the teacher's next login.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final SchedulingProperties props;

    public AdminBootstrap(UserRepository userRepository, SchedulingProperties props) {
        this.userRepository = userRepository;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        String adminEmail = props.getScheduling().getTeacherEmail();
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("AdminBootstrap — no app.scheduling.teacher-email configured; no admin promoted.");
            return;
        }
        int promoted = userRepository.promoteToAdminByEmail(adminEmail);
        if (promoted > 0) {
            log.info("AdminBootstrap — promoted '{}' to ADMIN.", adminEmail);
        } else {
            log.info("AdminBootstrap — '{}' already ADMIN or not yet registered.", adminEmail);
        }
    }
}
