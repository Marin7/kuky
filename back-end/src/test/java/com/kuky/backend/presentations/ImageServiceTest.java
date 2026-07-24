package com.kuky.backend.presentations;

import com.kuky.backend.presentations.exception.InvalidImageException;
import com.kuky.backend.presentations.model.Image;
import com.kuky.backend.presentations.repository.ImageRepository;
import com.kuky.backend.presentations.service.ImageFileStore;
import com.kuky.backend.presentations.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    private ImageRepository imageRepository;
    private ImageFileStore imageFileStore;
    private ImageService service;

    @BeforeEach
    void setUp() {
        imageRepository = mock(ImageRepository.class);
        imageFileStore = mock(ImageFileStore.class);
        service = new ImageService(imageRepository, imageFileStore);
    }

    @Test
    void storesValidPng() {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        var result = service.store(file);

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.byteSize()).isEqualTo(3);
        verify(imageFileStore).write(eq(result.id()), eq("image/png"), eq(new byte[]{1, 2, 3}));
        verify(imageRepository).insert(eq(result.id()), eq("image/png"), eq(3));
    }

    @Test
    void deletesFileWhenMetadataInsertFails() {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        doThrow(new RuntimeException("db down")).when(imageRepository).insert(any(), any(), anyInt());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
        verify(imageFileStore).deleteQuietly(any(UUID.class), eq("image/png"));
    }

    @Test
    void findLoadsBytesFromStore() {
        UUID id = UUID.randomUUID();
        Image meta = new Image();
        meta.setId(id);
        meta.setContentType("image/jpeg");
        meta.setByteSize(3);
        when(imageRepository.findById(id)).thenReturn(Optional.of(meta));
        when(imageFileStore.read(id, "image/jpeg")).thenReturn(Optional.of(new byte[]{9, 8, 7}));

        Optional<Image> found = service.find(id);

        assertThat(found).isPresent();
        assertThat(found.get().getData()).containsExactly(9, 8, 7);
    }

    @Test
    void findReturnsEmptyWhenFileMissing() {
        UUID id = UUID.randomUUID();
        Image meta = new Image();
        meta.setId(id);
        meta.setContentType("image/jpeg");
        meta.setByteSize(3);
        when(imageRepository.findById(id)).thenReturn(Optional.of(meta));
        when(imageFileStore.read(id, "image/jpeg")).thenReturn(Optional.empty());

        assertThat(service.find(id)).isEmpty();
    }

    @Test
    void rejectsDisallowedType() {
        var file = new MockMultipartFile("file", "a.gif", "image/gif", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidImageException.class);
        verify(imageRepository, never()).insert(any(), any(), anyInt());
        verify(imageFileStore, never()).write(any(), any(), any());
    }

    @Test
    void rejectsOversize() {
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("file", "big.jpg", "image/jpeg", big);
        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsEmpty() {
        var file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[]{});
        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidImageException.class);
    }
}
