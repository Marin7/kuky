package com.kuky.backend.scheduling;

import com.kuky.backend.admin.exception.UserNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.dto.MyBookingsResponse;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.exception.BookingNotFoundException;
import com.kuky.backend.scheduling.exception.SlotUnavailableException;
import com.kuky.backend.scheduling.meeting.MeetingProvider;
import com.kuky.backend.scheduling.model.Booking;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import com.kuky.backend.scheduling.service.BookingEmailService;
import com.kuky.backend.scheduling.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private AvailabilityService availabilityService;
    private MeetingProvider meetingProvider;
    private BookingEmailService emailService;
    private BookingService service;

    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);
        availabilityService = mock(AvailabilityService.class);
        meetingProvider = mock(MeetingProvider.class);
        emailService = mock(BookingEmailService.class);
        SchedulingProperties props = new SchedulingProperties(); // real defaults: 60/90 min, Madrid, etc.
        service = new BookingService(bookingRepository, userRepository, availabilityService,
                meetingProvider, emailService, props);
    }

    private Booking booking(String status, Instant slotStart) {
        Booking b = new Booking();
        b.setId(bookingId);
        b.setUserId(UUID.randomUUID());
        b.setStatus(status);
        b.setSlotStart(slotStart);
        b.setDurationMinutes(50);
        return b;
    }

    private User user(String email, boolean extendedClassEligible) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setExtendedClassEligible(extendedClassEligible);
        return u;
    }

    private User studentUser(String email, boolean extendedClassEligible) {
        User u = user(email, extendedClassEligible);
        u.setRole("STUDENT");
        return u;
    }

    @Test
    void setNoShowSucceedsForPastConfirmedBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS))));

        service.setNoShow(bookingId, true);

        verify(bookingRepository).setNoShow(bookingId, true);
    }

    @Test
    void setNoShowThrowsWhenBookingNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setNoShow(bookingId, true))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void setNoShowThrowsForFutureBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.setNoShow(bookingId, true))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW));
    }

    @Test
    void setNoShowThrowsForCancelledBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CANCELLED", Instant.now().minus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.setNoShow(bookingId, false))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW));
    }

    // --- createBooking: duration validation, eligibility, and pass-through (US2) -------------

    @Test
    void createBooking_90Minutes_succeedsForEligibleUser_andPassesDurationThrough() {
        String email = "eligible@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));
        when(bookingRepository.insert(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(bookingId);
            return b;
        });
        when(meetingProvider.create(eq(slotStart), eq(90), anyString()))
                .thenReturn(new MeetingProvider.MeetingDetails("zoom-1", "https://zoom.example/1"));

        var response = service.createBooking(email, slotStart, 90);

        assertThat(response.durationMinutes()).isEqualTo(90);
        verify(availabilityService).validateBookable(slotStart, 90);
        verify(meetingProvider).create(slotStart, 90, "Clase de español — " + email);
        verify(emailService).sendConfirmation(eq(email), anyString(), any(), eq(slotStart), eq(90), anyString());
    }

    @Test
    void createBooking_90Minutes_throwsNotEligible_forUserWithoutExtendedClassEligibility() {
        String email = "ineligible@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, false)));

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 90))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_EXTENDED));

        verify(bookingRepository, never()).insert(any());
    }

    @Test
    void createBooking_invalidDuration_throwsInvalidDuration() {
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 45))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.INVALID_DURATION));

        verify(bookingRepository, never()).insert(any());
    }

    @Test
    void createBooking_60Minutes_doesNotRequireEligibility() {
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, false)));
        when(bookingRepository.insert(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(bookingId);
            return b;
        });
        when(meetingProvider.create(eq(slotStart), eq(60), anyString()))
                .thenReturn(new MeetingProvider.MeetingDetails("zoom-2", "https://zoom.example/2"));

        var response = service.createBooking(email, slotStart, 60);

        assertThat(response.durationMinutes()).isEqualTo(60);
    }

    @Test
    void createBooking_overlapExclusionViolation_isTranslatedToSlotUnavailable() {
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));
        when(bookingRepository.insert(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("exclusion constraint violated"));

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 90))
                .isInstanceOf(SlotUnavailableException.class);
    }

    // --- 15-minute booking buffer (spec 020, US1/US2) -----------------------------------------

    @Test
    void createBooking_propagatesBufferRejection_asSlotUnavailable() {
        // AvailabilityService.validateBookable() is the sole gate for the buffer rule (spec 020) —
        // this proves BookingService neither catches nor bypasses that rejection before insert.
        String email = "student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));
        doThrow(new SlotUnavailableException("Esta hora ya ha sido reservada."))
                .when(availabilityService).validateBookable(slotStart, 60);

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 60))
                .isInstanceOf(SlotUnavailableException.class);

        verify(bookingRepository, never()).insert(any());
    }

    @Test
    void createBooking_bufferRejectionAppliesEvenWhenTheAdjacentBookingBelongsToTheSameStudent() {
        // BookingRepository.BookedInterval (consumed by validateBookable) carries no user id at all —
        // the buffer check structurally cannot distinguish "my own earlier class" from anyone else's,
        // so the rejection path here is identical regardless of whose booking triggered it (US2).
        String email = "repeat-student@example.com";
        Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(user(email, true)));
        doThrow(new SlotUnavailableException("Esta hora ya ha sido reservada."))
                .when(availabilityService).validateBookable(slotStart, 60);

        assertThatThrownBy(() -> service.createBooking(email, slotStart, 60))
                .isInstanceOf(SlotUnavailableException.class);

        verify(meetingProvider, never()).create(any(), anyInt(), anyString());
    }

    // --- attachCompanionStudent (US1) -------------------------------------------------------------

    @Test
    void attachCompanionStudent_succeeds_forValidStudentTarget() {
        Booking existing = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        existing.setDurationMinutes(60);
        existing.setZoomJoinUrl("https://zoom.example/1");
        User target = studentUser("second@example.com", false);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        Booking result = service.attachCompanionStudent(bookingId, target.getId());

        assertThat(result.getCompanionStudentId()).isEqualTo(target.getId());
        verify(bookingRepository).setCompanionStudentId(bookingId, target.getId());
        verify(emailService).sendCompanionStudentAttached(eq("second@example.com"), eq(bookingId),
                eq(existing.getSlotStart()), eq(60), eq("https://zoom.example/1"));
    }

    @Test
    void attachCompanionStudent_throwsBookingNotAttachable_forCancelledBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CANCELLED", Instant.now().plus(1, ChronoUnit.DAYS))));
        User target = studentUser("second@example.com", false);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, target.getId()))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.BOOKING_NOT_ATTACHABLE));
    }

    @Test
    void attachCompanionStudent_throwsBookingNotAttachable_forPastBooking() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS))));
        User target = studentUser("second@example.com", false);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, target.getId()))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.BOOKING_NOT_ATTACHABLE));
    }

    @Test
    void attachCompanionStudent_throwsAlreadyAttached_whenSecondStudentAlreadySet() {
        Booking existing = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        existing.setCompanionStudentId(UUID.randomUUID());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        User target = studentUser("third@example.com", false);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, target.getId()))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.COMPANION_ALREADY_ATTACHED));
    }

    @Test
    void attachCompanionStudent_throwsSameAsPrimary_whenTargetIsThePrimaryStudent() {
        Booking existing = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        User target = studentUser("primary@example.com", false);
        target.setId(existing.getUserId());
        when(userRepository.findById(existing.getUserId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, existing.getUserId()))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.COMPANION_SAME_AS_BOOKING_STUDENT));
    }

    @Test
    void attachCompanionStudent_throwsNotStudent_forNonStudentTarget() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS))));
        User target = user("plain-user@example.com", false); // role defaults to "USER"
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, target.getId()))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.COMPANION_NOT_STUDENT));
    }

    @Test
    void attachCompanionStudent_throwsNotEligibleForExtended_whenBookingIsExtendedAndTargetIsNotEligible() {
        Booking existing = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        existing.setDurationMinutes(90);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        User target = studentUser("second@example.com", false);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, target.getId()))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_EXTENDED));
    }

    @Test
    void attachCompanionStudent_succeeds_whenBookingIsExtendedAndTargetIsEligible() {
        Booking existing = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        existing.setDurationMinutes(90);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        User target = studentUser("second@example.com", true);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        service.attachCompanionStudent(bookingId, target.getId());

        verify(bookingRepository).setCompanionStudentId(bookingId, target.getId());
    }

    @Test
    void attachCompanionStudent_throwsUserNotFound_forUnknownTargetId() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS))));
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachCompanionStudent(bookingId, unknownId))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- listForUser / cancelBooking with a companion student (US2) --------------------------------

    @Test
    void listForUser_includesBookingsWhereUserIsOnlyTheSecondStudent() {
        String secondEmail = "second@example.com";
        UUID secondId = UUID.randomUUID();
        User second = studentUser(secondEmail, false);
        second.setId(secondId);
        when(userRepository.findByEmailIgnoreCase(secondEmail)).thenReturn(Optional.of(second));

        Booking asSecond = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        asSecond.setCompanionStudentId(secondId);
        when(bookingRepository.findByUserId(secondId)).thenReturn(List.of(asSecond));

        MyBookingsResponse response = service.listForUser(secondEmail);

        assertThat(response.upcoming()).hasSize(1);
        assertThat(response.upcoming().get(0).isCompanionStudent()).isTrue();
    }

    @Test
    void listForUser_marksOwnBookingsAsNotSecondStudent() {
        String email = "primary@example.com";
        UUID userId = UUID.randomUUID();
        User primary = studentUser(email, false);
        primary.setId(userId);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(primary));

        Booking own = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        own.setUserId(userId);
        when(bookingRepository.findByUserId(userId)).thenReturn(List.of(own));

        MyBookingsResponse response = service.listForUser(email);

        assertThat(response.upcoming().get(0).isCompanionStudent()).isFalse();
    }

    @Test
    void cancelBooking_bySecondStudent_succeedsAndNotifiesBothStudents() {
        String primaryEmail = "primary@example.com";
        UUID primaryId = UUID.randomUUID();
        String secondEmail = "second@example.com";
        UUID secondId = UUID.randomUUID();

        Booking shared = booking("CONFIRMED", Instant.now().plus(2, ChronoUnit.DAYS));
        shared.setUserId(primaryId);
        shared.setCompanionStudentId(secondId);

        User second = studentUser(secondEmail, false);
        second.setId(secondId);
        when(userRepository.findByEmailIgnoreCase(secondEmail)).thenReturn(Optional.of(second));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(shared));
        User primary = studentUser(primaryEmail, false);
        primary.setId(primaryId);
        when(userRepository.findById(primaryId)).thenReturn(Optional.of(primary));

        service.cancelBooking(secondEmail, bookingId);

        verify(bookingRepository).markCancelled(eq(bookingId), any());
        verify(emailService).sendCancellation(anyString(), eq(secondEmail), eq(bookingId),
                any(), anyInt(), any(), eq(primaryEmail));
    }

    @Test
    void cancelBooking_byPrimaryStudent_onSharedBooking_alsoNotifiesSecondStudent() {
        String primaryEmail = "primary@example.com";
        UUID primaryId = UUID.randomUUID();
        String secondEmail = "second@example.com";
        UUID secondId = UUID.randomUUID();

        Booking shared = booking("CONFIRMED", Instant.now().plus(2, ChronoUnit.DAYS));
        shared.setUserId(primaryId);
        shared.setCompanionStudentId(secondId);

        User primary = studentUser(primaryEmail, false);
        primary.setId(primaryId);
        when(userRepository.findByEmailIgnoreCase(primaryEmail)).thenReturn(Optional.of(primary));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(shared));
        User second = studentUser(secondEmail, false);
        second.setId(secondId);
        when(userRepository.findById(secondId)).thenReturn(Optional.of(second));

        service.cancelBooking(primaryEmail, bookingId);

        verify(emailService).sendCancellation(anyString(), eq(primaryEmail), eq(bookingId),
                any(), anyInt(), any(), eq(secondEmail));
    }

    @Test
    void cancelBooking_onNonSharedBooking_passesNullForOtherStudentEmail() {
        String email = "solo@example.com";
        UUID userId = UUID.randomUUID();
        User user = studentUser(email, false);
        user.setId(userId);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        Booking solo = booking("CONFIRMED", Instant.now().plus(2, ChronoUnit.DAYS));
        solo.setUserId(userId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(solo));

        service.cancelBooking(email, bookingId);

        verify(emailService).sendCancellation(anyString(), eq(email), eq(bookingId),
                any(), anyInt(), any(), isNull());
    }

    // --- detachCompanionStudent / per-student no-show (US3) ----------------------------------------

    @Test
    void detachCompanionStudent_clearsCompanion_whenOnePresent() {
        Booking shared = booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        shared.setCompanionStudentId(UUID.randomUUID());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(shared));

        service.detachCompanionStudent(bookingId);

        verify(bookingRepository).clearCompanionStudentId(bookingId);
    }

    @Test
    void detachCompanionStudent_throwsNotAttached_whenNoCompanion() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.detachCompanionStudent(bookingId))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.COMPANION_NOT_ATTACHED));
    }

    @Test
    void setNoShow_targetingCompanion_setsOnlyTheCompanionColumn() {
        Booking shared = booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));
        shared.setCompanionStudentId(UUID.randomUUID());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(shared));

        service.setNoShow(bookingId, true, "COMPANION");

        verify(bookingRepository).setCompanionStudentNoShow(bookingId, true);
        verify(bookingRepository, never()).setNoShow(any(), anyBoolean());
    }

    @Test
    void setNoShow_targetingBookingStudent_setsOnlyTheBookingStudentColumn() {
        Booking shared = booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));
        shared.setCompanionStudentId(UUID.randomUUID());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(shared));

        service.setNoShow(bookingId, true, "BOOKING_STUDENT");

        verify(bookingRepository).setNoShow(bookingId, true);
        verify(bookingRepository, never()).setCompanionStudentNoShow(any(), anyBoolean());
    }

    @Test
    void setNoShow_targetingCompanion_throwsNotAttached_whenNoCompanion() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.setNoShow(bookingId, true, "COMPANION"))
                .isInstanceOf(BookingNotAllowedException.class)
                .satisfies(ex -> assertThat(((BookingNotAllowedException) ex).getReason())
                        .isEqualTo(BookingNotAllowedException.Reason.COMPANION_NOT_ATTACHED));
    }

    @Test
    void setNoShow_withoutStudentRole_defaultsToBookingStudent_unchangedFromBeforeThisFeature() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS))));

        service.setNoShow(bookingId, true);

        verify(bookingRepository).setNoShow(bookingId, true);
    }
}
