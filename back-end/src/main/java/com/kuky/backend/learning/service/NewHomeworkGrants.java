package com.kuky.backend.learning.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Homework that became newly available to students during a single teacher action.
 *
 * <p>Transient and request-scoped: one instance per service method invocation, flushed once
 * at the end of that method. Keyed by student so each student receives exactly one email
 * however many homework the action granted them, and so no message can ever name two
 * recipients. Insertion-ordered so the listing in the email is deterministic.
 */
public final class NewHomeworkGrants {

    private final Map<UUID, List<UUID>> byStudent = new LinkedHashMap<>();

    /** Records that {@code assignmentId} is newly available to each of {@code newlyGrantedUserIds}. */
    public void add(UUID assignmentId, Collection<UUID> newlyGrantedUserIds) {
        if (assignmentId == null || newlyGrantedUserIds == null || newlyGrantedUserIds.isEmpty()) {
            return;
        }
        for (UUID userId : newlyGrantedUserIds) {
            if (userId == null) {
                continue;
            }
            List<UUID> assignments = byStudent.computeIfAbsent(userId, k -> new ArrayList<>());
            if (!assignments.contains(assignmentId)) {
                assignments.add(assignmentId);
            }
        }
    }

    public boolean isEmpty() {
        return byStudent.isEmpty();
    }

    /** Student id → the homework newly granted to them, in the order it was granted. */
    public Map<UUID, List<UUID>> byStudent() {
        return byStudent;
    }
}
