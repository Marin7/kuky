package com.kuky.backend.presentations;

import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Component;

/**
 * Idempotent remap of legacy on-disk blobs keyed by presentation id to file id.
 * Runs after Flyway; no-ops if the multi-file schema is not yet available.
 */
@Component
@Order
public class PresentationFileDiskMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PresentationFileDiskMigrator.class);

    private final PresentationRepository repository;
    private final PresentationFileStore fileStore;

    public PresentationFileDiskMigrator(PresentationRepository repository, PresentationFileStore fileStore) {
        this.repository = repository;
        this.fileStore = fileStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int checked = 0;
            for (PresentationFile row : repository.listAllFileRows()) {
                fileStore.renameLegacyIfNeeded(row.presentationId(), row.id());
                checked++;
            }
            if (checked > 0) {
                log.debug("Presentation file disk migrator checked {} row(s)", checked);
            }
        } catch (BadSqlGrammarException e) {
            log.warn("Skipping presentation file disk remap (schema not ready): {}",
                    e.getMostSpecificCause().getMessage());
        }
    }
}
