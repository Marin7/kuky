package com.kuky.backend.auth.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public boolean tryConsume(String ipAddress) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        Deque<Long> timestamps = attempts.computeIfAbsent(ipAddress, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
