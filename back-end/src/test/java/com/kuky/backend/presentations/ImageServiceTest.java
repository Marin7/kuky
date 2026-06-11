package com.kuky.backend.presentations;

import com.kuky.backend.presentations.exception.InvalidImageException;
import com.kuky.backend.presentations.repository.ImageRepository;
import com.kuky.backend.presentations.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    private ImageRepository imageRepository;
    private ImageService service;

    @BeforeEach
    void setUp() {
        imageRepository = mock(ImageRepository.class);
        service = new ImageService(imageRepository);
    }

    @Test
    void storesValidPng() {
        UUID id = UUID.randomUUID();
        when(imageRepository.insert(eq("image/png"), any())).thenReturn(id);
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        var result = service.store(file);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.byteSize()).isEqualTo(3);
    }

    @Test
    void rejectsDisallowedType() {
        var file = new MockMultipartFile("file", "a.gif", "image/gif", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidImageException.class);
        verify(imageRepository, never()).insert(any(), any());
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
