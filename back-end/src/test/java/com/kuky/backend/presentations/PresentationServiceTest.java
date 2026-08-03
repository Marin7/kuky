package com.kuky.backend.presentations;

import com.kuky.backend.admin.dto.PresentationDetail;
import com.kuky.backend.admin.dto.PresentationFileSummary;
import com.kuky.backend.admin.dto.SlideRequest;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.Presentation;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationFileStore;
import com.kuky.backend.presentations.service.PresentationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PresentationServiceTest {

    private PresentationRepository repository;
    private UserRepository userRepository;
    private PresentationFileStore fileStore;
    private PresentationService service;

    private final UUID deckId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(PresentationRepository.class);
        userRepository = mock(UserRepository.class);
        fileStore = mock(PresentationFileStore.class);
        service = new PresentationService(repository, userRepository, fileStore);

        Presentation p = new Presentation();
        p.setId(deckId);
        p.setTitle("Clase 1");
        when(repository.findById(deckId)).thenReturn(Optional.of(p));
        when(repository.findSharedUsers(deckId)).thenReturn(List.of());
        when(repository.listFiles(deckId)).thenReturn(List.of());
        when(repository.listDisplayNames(deckId)).thenReturn(List.of());
        when(repository.countFiles(deckId)).thenReturn(0);
    }

    @Test
    void reorderRejectsNonPermutation() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repository.findSlideIds(deckId)).thenReturn(List.of(a, b));

        assertThatThrownBy(() -> service.reorder(deckId, List.of(a)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.reorder(deckId, List.of(a, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).updateSortOrders(any(), any());
    }

    @Test
    void reorderAcceptsValidPermutation() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repository.findSlideIds(deckId)).thenReturn(List.of(a, b));
        when(repository.findSlides(deckId)).thenReturn(List.of());

        service.reorder(deckId, List.of(b, a));

        verify(repository).updateSortOrders(deckId, List.of(b, a));
    }

    @Test
    void addSlideRejectedWhenCapReached() {
        List<UUID> hundred = IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).toList();
        when(repository.findSlideIds(deckId)).thenReturn(hundred);

        assertThatThrownBy(() -> service.addSlide(deckId, new SlideRequest("H", "B", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void operationsOnMissingDeckThrowNotFound() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(missing))
                .isInstanceOf(PresentationNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(repository.delete(deckId)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(deckId))
                .isInstanceOf(PresentationNotFoundException.class);
    }

    @Test
    void uploadAddsWithoutReplacingExisting() throws Exception {
        when(repository.countFiles(deckId)).thenReturn(1);
        when(repository.listDisplayNames(deckId)).thenReturn(List.of("first.pptx"));
        UUID fileId = UUID.randomUUID();
        when(repository.listFiles(deckId)).thenReturn(List.of(
                new PresentationFileSummary(fileId, "first.pptx", "first.pptx",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        10, Instant.now()),
                new PresentationFileSummary(UUID.randomUUID(), "second.pptx", "second.pptx",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        10, Instant.now())));

        MockMultipartFile file = new MockMultipartFile(
                "file", "second.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                new byte[]{1, 2, 3});

        PresentationDetail detail = service.uploadFile(deckId, file);

        verify(repository).insertFile(any(UUID.class), eq(deckId), eq("second.pptx"),
                eq("second.pptx"), anyString(), eq(3));
        verify(repository, never()).deleteFile(any(), any());
        assertThat(detail.files()).hasSize(2);
    }

    @Test
    void uploadAssignsSuffixedDisplayNameOnCollision() throws Exception {
        when(repository.listDisplayNames(deckId)).thenReturn(List.of("deck.pptx"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "deck.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                new byte[]{1});

        service.uploadFile(deckId, file);

        verify(repository).insertFile(any(UUID.class), eq(deckId), eq("deck.pptx"),
                eq("deck (2).pptx"), anyString(), eq(1));
    }

    @Test
    void allocateDisplayNameKeepsOriginalWhenFree() {
        assertThat(PresentationService.allocateDisplayName("a.pptx", Set.of()))
                .isEqualTo("a.pptx");
    }

    @Test
    void allocateDisplayNameSkipsUsedSuffixes() {
        Set<String> used = new HashSet<>(List.of("a.pptx", "a (2).pptx"));
        assertThat(PresentationService.allocateDisplayName("a.pptx", used))
                .isEqualTo("a (3).pptx");
    }

    @Test
    void uploadRejectsAtTenFiles() {
        when(repository.countFiles(deckId)).thenReturn(10);
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                new byte[]{1});

        assertThatThrownBy(() -> service.uploadFile(deckId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10");
        verify(repository, never()).insertFile(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void removeFileDeletesOnlyThatFile() {
        UUID fileId = UUID.randomUUID();
        when(repository.deleteFile(deckId, fileId)).thenReturn(1);

        service.removeFile(deckId, fileId);

        verify(repository).deleteFile(deckId, fileId);
        verify(fileStore).deleteQuietly(fileId);
        verify(repository).touch(deckId);
    }

    @Test
    void getFileDataReturnsBytesByFileId() {
        UUID fileId = UUID.randomUUID();
        PresentationFile meta = new PresentationFile(
                fileId, deckId, "deck.pptx", "deck.pptx",
                "application/pdf", 3, Instant.now(), null);
        when(repository.findFile(deckId, fileId)).thenReturn(Optional.of(meta));
        when(fileStore.read(fileId)).thenReturn(Optional.of(new byte[]{9, 9, 9}));

        PresentationFile result = service.getFileData(deckId, fileId);

        assertThat(result.displayName()).isEqualTo("deck.pptx");
        assertThat(result.data()).containsExactly(9, 9, 9);
    }

    @Test
    void deletePresentationRemovesAllFileBlobs() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repository.listFileIds(deckId)).thenReturn(List.of(a, b));
        when(repository.delete(deckId)).thenReturn(1);

        service.delete(deckId);

        verify(fileStore).deleteQuietly(a);
        verify(fileStore).deleteQuietly(b);
    }
}
