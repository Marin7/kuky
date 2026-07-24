package com.kuky.backend.presentations;

import com.kuky.backend.config.ImageProperties;
import com.kuky.backend.presentations.service.ImageFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFileStoreTest {

    private static final UUID SEED_ID = UUID.fromString("e30ad774-4f18-4823-8805-588928ca55a0");

    @TempDir
    Path tempDir;

    @Test
    void readsSeedAvatarFromClasspath() {
        ImageProperties props = new ImageProperties();
        props.setStorageDir(tempDir.resolve("empty").toString());
        ImageFileStore store = new ImageFileStore(props);

        Optional<byte[]> bytes = store.read(SEED_ID, "image/jpeg");

        assertThat(bytes).isPresent();
        assertThat(bytes.get()).hasSize(244549);
        assertThat(bytes.get()[0] & 0xFF).isEqualTo(0xFF);
        assertThat(bytes.get()[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    void prefersDiskOverClasspath() {
        ImageProperties props = new ImageProperties();
        props.setStorageDir(tempDir.toString());
        ImageFileStore store = new ImageFileStore(props);
        byte[] onDisk = new byte[]{1, 2, 3, 4};

        store.write(SEED_ID, "image/jpeg", onDisk);

        assertThat(store.read(SEED_ID, "image/jpeg")).contains(onDisk);
    }
}
