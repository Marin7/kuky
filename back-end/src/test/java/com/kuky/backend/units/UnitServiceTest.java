package com.kuky.backend.units;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.quiz.exception.QuizNotFoundException;
import com.kuky.backend.quiz.model.Quiz;
import com.kuky.backend.quiz.repository.QuizAssigneeRepository;
import com.kuky.backend.quiz.repository.QuizQuestionRepository;
import com.kuky.backend.quiz.repository.QuizRepository;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import com.kuky.backend.units.service.UnitPdfService;
import com.kuky.backend.units.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UnitService: end-of-unit quizzes (linking, moving, assignment sync) and page titles. */
class UnitServiceTest {

    private UnitRepository unitRepository;
    private UserRepository userRepository;
    private QuizRepository quizRepository;
    private QuizQuestionRepository quizQuestionRepository;
    private QuizAssigneeRepository quizAssigneeRepository;
    private UnitService service;
    private PresentationRepository presentationRepository;

    private final UUID unitId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        unitRepository = mock(UnitRepository.class);
        userRepository = mock(UserRepository.class);
        presentationRepository = mock(PresentationRepository.class);
        quizRepository = mock(QuizRepository.class);
        quizQuestionRepository = mock(QuizQuestionRepository.class);
        quizAssigneeRepository = mock(QuizAssigneeRepository.class);
        service = new UnitService(unitRepository, userRepository, presentationRepository,
                mock(UnitPdfService.class), quizRepository, quizQuestionRepository, quizAssigneeRepository);

        Unit unit = new Unit();
        unit.setId(unitId);
        unit.setLevel("A1");
        unit.setSubject("La familia");
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        lenient().when(presentationRepository.findOwnedByUnit(any())).thenReturn(Optional.empty());
        lenient().when(unitRepository.findAssigneeIds(unitId)).thenReturn(List.of(alice));
        lenient().when(quizRepository.findById(any())).thenAnswer(inv -> Optional.of(new Quiz()));
        lenient().when(quizQuestionRepository.countLive(any())).thenReturn(3);
        for (UUID id : List.of(alice, bob)) {
            User u = new User();
            u.setId(id);
            u.setRole("STUDENT");
            lenient().when(userRepository.findById(id)).thenReturn(Optional.of(u));
        }
    }

    @Test
    void linkingQuizzesAssignsThemToTheUnitsStudentsInOrder() {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        when(unitRepository.findQuizzes(unitId)).thenReturn(List.of());

        service.setQuizzes(unitId, List.of(q2, q1, q2));

        verify(unitRepository).setQuizzes(unitId, List.of(q2, q1));
        verify(quizAssigneeRepository).addUnitAssignees(q1, List.of(alice));
        verify(quizAssigneeRepository).addUnitAssignees(q2, List.of(alice));
    }

    @Test
    void droppingAQuizUnassignsItFromStudentsWhoHaveNotStarted() {
        UUID kept = UUID.randomUUID();
        UUID dropped = UUID.randomUUID();
        when(unitRepository.findQuizzes(unitId)).thenReturn(List.of(
                new UnitRepository.UnitQuiz(kept, "K", 1), new UnitRepository.UnitQuiz(dropped, "D", 1)));

        service.setQuizzes(unitId, List.of(kept));

        verify(quizAssigneeRepository).removeUnstartedAssignees(dropped, List.of(alice));
        verify(quizAssigneeRepository, never()).removeUnstartedAssignees(eq(kept), anyList());
    }

    @Test
    void movingAQuizFromAnotherUnitUnassignsThatUnitsStudents() {
        UUID otherUnit = UUID.randomUUID();
        UUID quiz = UUID.randomUUID();
        when(unitRepository.findQuizzes(unitId)).thenReturn(List.of());
        when(unitRepository.findUnitIdForQuiz(quiz)).thenReturn(Optional.of(otherUnit));
        when(unitRepository.findAssigneeIds(otherUnit)).thenReturn(List.of(bob));

        service.setQuizzes(unitId, List.of(quiz));

        verify(quizAssigneeRepository).removeUnstartedAssignees(quiz, List.of(bob));
        verify(quizAssigneeRepository).addUnitAssignees(quiz, List.of(alice));
    }

    @Test
    void aQuizWithoutQuestionsCannotBeLinked() {
        UUID empty = UUID.randomUUID();
        when(quizQuestionRepository.countLive(empty)).thenReturn(0);

        assertThatThrownBy(() -> service.setQuizzes(unitId, List.of(empty)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(unitRepository, never()).setQuizzes(any(), anyList());
    }

    @Test
    void anUnknownQuizIsRejected() {
        UUID missing = UUID.randomUUID();
        when(quizRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setQuizzes(unitId, List.of(missing)))
                .isInstanceOf(QuizNotFoundException.class);
    }

    @Test
    void changingAssigneesGrantsAndRevokesTheUnitsQuizzes() {
        UUID quiz = UUID.randomUUID();
        when(unitRepository.findQuizzes(unitId)).thenReturn(List.of(new UnitRepository.UnitQuiz(quiz, "Q", 1)));

        service.setAssignees(unitId, List.of(bob));

        verify(unitRepository).replaceAssignees(unitId, List.of(bob));
        verify(quizAssigneeRepository).addUnitAssignees(quiz, List.of(bob));
        verify(quizAssigneeRepository).removeUnstartedAssignees(quiz, List.of(alice));
    }

    @Test
    void renamingAPageTrimsTheTitleAndRejectsBlankOrOutOfRange() {
        UUID pdf = UUID.randomUUID();
        when(presentationRepository.findOwnedByUnit(unitId)).thenReturn(Optional.of(
                new PresentationRepository.OwnedPdf(pdf, UUID.randomUUID(), "u.pdf", 10, 5)));
        when(presentationRepository.setPageTitle(pdf, 3, "Los saludos")).thenReturn(1);

        service.setPageTitle(unitId, 3, "  Los saludos  ");
        verify(presentationRepository).setPageTitle(pdf, 3, "Los saludos");

        assertThatThrownBy(() -> service.setPageTitle(unitId, 3, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setPageTitle(unitId, 6, "Fuera"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
