package com.kuky.backend.units;

import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationFileStore;
import com.kuky.backend.units.exception.ActivitiesOutOfRangeException;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import com.kuky.backend.units.service.UnitPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Uploading / replacing a unit's single PDF. No transaction is active, so after-commit work runs inline. */
class UnitPdfServiceTest {

    private UnitRepository unitRepository;
    private PresentationRepository presentationRepository;
    private ActivityRepository activityRepository;
    private PresentationFileStore fileStore;
    private UnitPdfService service;

    private final UUID unitId = UUID.randomUUID();
    private final UUID presentationId = UUID.randomUUID();
    private final UUID oldFileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        unitRepository = mock(UnitRepository.class);
        presentationRepository = mock(PresentationRepository.class);
        activityRepository = mock(ActivityRepository.class);
        fileStore = mock(PresentationFileStore.class);
        service = new UnitPdfService(unitRepository, presentationRepository, activityRepository, fileStore);

        Unit unit = new Unit();
        unit.setId(unitId);
        unit.setLevel("A1");
        unit.setSubject("La familia");
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
    }

    private static MultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf",
                "%PDF-1.7 minimal".getBytes(StandardCharsets.US_ASCII));
    }

    private void givenExistingPdf() {
        when(presentationRepository.findOwnedByUnit(unitId)).thenReturn(Optional.of(
                new PresentationRepository.OwnedPdf(presentationId, oldFileId, "old.pdf", 10, 10, 0)));
    }

    @Test
    void rejectsEmptyNonPdfAndBadPageCounts() {
        MultipartFile empty = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]);
        MultipartFile docx = new MockMultipartFile("file", "x.docx", "application/msword", new byte[]{1, 2});
        MultipartFile fakePdf = new MockMultipartFile("file", "x.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.upload(unitId, empty, 3, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(unitId, docx, 3, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(unitId, fakePdf, 3, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(unitId, pdf("a.pdf"), 0, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(unitId, pdf("a.pdf"), 501, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upload(unitId, pdf("a.pdf"), null, false)).isInstanceOf(IllegalArgumentException.class);
        verify(fileStore, never()).write(any(), any());
    }

    @Test
    void firstUploadCreatesTheOwnedPresentationAtTheEndOfTheSequence() {
        when(presentationRepository.findOwnedByUnit(unitId)).thenReturn(Optional.empty());
        when(unitRepository.nextContentPosition(unitId)).thenReturn(2);
        when(presentationRepository.insertOwned(unitId, "La familia", "A1", 2)).thenReturn(presentationId);

        service.upload(unitId, pdf("familia.pdf"), 12, false);

        verify(presentationRepository).insertOwned(unitId, "La familia", "A1", 2);
        verify(presentationRepository).insertFile(any(), eq(presentationId), eq("familia.pdf"), eq("familia.pdf"),
                eq("application/pdf"), anyInt(), eq(12));
    }

    @Test
    void replacingWithEnoughPagesKeepsActivitiesAndDropsTheOldFile() {
        givenExistingPdf();
        when(activityRepository.countBeyondPage(presentationId, 12)).thenReturn(0);

        service.upload(unitId, pdf("new.pdf"), 12, false);

        verify(activityRepository, never()).deleteBeyondPage(any(), anyInt());
        verify(presentationRepository).deleteFile(presentationId, oldFileId);
        verify(fileStore).deleteQuietly(oldFileId);
    }

    @Test
    void replacingWithFewerPagesNeedsConfirmationAndWritesNothingWithoutIt() {
        givenExistingPdf();
        when(activityRepository.countBeyondPage(presentationId, 6)).thenReturn(1);

        assertThatThrownBy(() -> service.upload(unitId, pdf("short.pdf"), 6, false))
                .isInstanceOf(ActivitiesOutOfRangeException.class);
        verify(fileStore, never()).write(any(), any());
        verify(presentationRepository, never()).deleteFile(any(), any());
    }

    @Test
    void replacingWithFewerPagesAndConfirmationDeletesOutOfRangeActivities() {
        givenExistingPdf();
        when(activityRepository.countBeyondPage(presentationId, 6)).thenReturn(1);

        service.upload(unitId, pdf("short.pdf"), 6, true);

        verify(activityRepository).deleteBeyondPage(presentationId, 6);
        verify(presentationRepository).insertFile(any(), eq(presentationId), any(), any(), any(), anyInt(), eq(6));
    }
}
