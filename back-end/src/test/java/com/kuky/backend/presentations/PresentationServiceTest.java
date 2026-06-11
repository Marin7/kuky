package com.kuky.backend.presentations;

import com.kuky.backend.admin.dto.SlideRequest;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.Presentation;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PresentationServiceTest {

    private PresentationRepository repository;
    private UserRepository userRepository;
    private PresentationService service;

    private final UUID deckId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(PresentationRepository.class);
        userRepository = mock(UserRepository.class);
        service = new PresentationService(repository, userRepository);

        Presentation p = new Presentation();
        p.setId(deckId);
        p.setTitle("Clase 1");
        when(repository.findById(deckId)).thenReturn(Optional.of(p));
        when(repository.findSharedUsers(deckId)).thenReturn(List.of());
    }

    @Test
    void reorderRejectsNonPermutation() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repository.findSlideIds(deckId)).thenReturn(List.of(a, b));

        assertThatThrownBy(() -> service.reorder(deckId, List.of(a))) // missing b
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.reorder(deckId, List.of(a, UUID.randomUUID()))) // unknown id
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
}
