package com.kuky.backend.config;

import com.kuky.backend.scheduling.model.AvailabilityException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.DayWindow;
import com.kuky.backend.scheduling.repository.AvailabilityRepository;
import com.kuky.backend.scheduling.service.AvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time, behavior-preserving migration for feature 009. When no weeks are yet materialized,
 * snapshots the current bookable horizon from today's effective availability
 * ({@code rules ∪ OPEN − BLOCK}) so the launch schedule is identical to the pre-feature one
 * (FR-011). After this runs, runtime materialization seeds future weeks from the template only,
 * and the legacy {@code availability_exceptions} table is no longer read at runtime.
 *
 * <p>The interval merge/subtract below intentionally duplicates the legacy delta semantics so the
 * runtime path stays free of them; this class is removable a release after rollout.
 */
@Component
public class WeekAvailabilityBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WeekAvailabilityBootstrap.class);

    private final AvailabilityRepository repository;
    private final SchedulingProperties props;
    private final Clock clock;

    public WeekAvailabilityBootstrap(AvailabilityRepository repository,
                                     SchedulingProperties props,
                                     Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (repository.hasAnyMaterializedWeek()) {
            return; // already migrated
        }
        ZoneId zone = ZoneId.of(props.getScheduling().getTeacherTimezone());
        LocalDate horizonStart = LocalDate.now(clock.withZone(zone))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate horizonEnd = horizonStart.plusWeeks(AvailabilityService.HORIZON_WEEKS);

        List<AvailabilityRule> rules = repository.findAllRules();
        List<AvailabilityException> exceptions = repository.findExceptionsBetween(horizonStart, horizonEnd);

        int weeks = 0;
        for (LocalDate week = horizonStart; week.isBefore(horizonEnd); week = week.plusWeeks(1)) {
            List<DayWindow> windows = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate date = week.plusDays(i);
                for (Interval iv : effectiveIntervals(date, rules, exceptions)) {
                    windows.add(new DayWindow(date, iv.start, iv.end));
                }
            }
            if (repository.materializeWeek(week, windows)) {
                weeks++;
            }
        }
        log.info("WeekAvailabilityBootstrap — materialized {} week(s) from existing rules/exceptions.", weeks);
    }

    /** Legacy effective availability for a date: merge(rules + OPEN) − BLOCK. */
    private List<Interval> effectiveIntervals(LocalDate date, List<AvailabilityRule> rules,
                                              List<AvailabilityException> exceptions) {
        int iso = date.getDayOfWeek().getValue();
        List<Interval> positives = new ArrayList<>();
        for (AvailabilityRule r : rules) {
            if (r.getDayOfWeek() == iso) {
                positives.add(new Interval(r.getStartTime(), r.getEndTime()));
            }
        }
        for (AvailabilityException e : exceptions) {
            if (e.getDate().equals(date) && e.getKind() == AvailabilityException.Kind.OPEN) {
                positives.add(new Interval(e.getStartTime(), e.getEndTime()));
            }
        }
        List<Interval> merged = merge(positives);

        List<Interval> blocks = new ArrayList<>();
        for (AvailabilityException e : exceptions) {
            if (e.getDate().equals(date) && e.getKind() == AvailabilityException.Kind.BLOCK) {
                blocks.add(new Interval(e.getStartTime(), e.getEndTime()));
            }
        }
        return subtract(merged, blocks);
    }

    private static List<Interval> merge(List<Interval> intervals) {
        if (intervals.isEmpty()) return List.of();
        List<Interval> sorted = new ArrayList<>(intervals);
        sorted.sort((a, b) -> a.start.compareTo(b.start));
        List<Interval> out = new ArrayList<>();
        Interval cur = new Interval(sorted.get(0).start, sorted.get(0).end);
        for (int i = 1; i < sorted.size(); i++) {
            Interval nxt = sorted.get(i);
            if (!nxt.start.isAfter(cur.end)) {
                if (nxt.end.isAfter(cur.end)) cur = new Interval(cur.start, nxt.end);
            } else {
                out.add(cur);
                cur = new Interval(nxt.start, nxt.end);
            }
        }
        out.add(cur);
        return out;
    }

    private static List<Interval> subtract(List<Interval> base, List<Interval> blocks) {
        List<Interval> result = new ArrayList<>(base);
        for (Interval block : blocks) {
            List<Interval> next = new ArrayList<>();
            for (Interval iv : result) {
                if (!block.start.isBefore(iv.end) || !block.end.isAfter(iv.start)) {
                    next.add(iv);
                    continue;
                }
                if (block.start.isAfter(iv.start)) {
                    next.add(new Interval(iv.start, block.start));
                }
                if (block.end.isBefore(iv.end)) {
                    next.add(new Interval(block.end, iv.end));
                }
            }
            result = next;
        }
        return result;
    }

    private record Interval(LocalTime start, LocalTime end) {}
}
