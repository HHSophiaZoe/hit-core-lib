package com.hit.websocket.client.observability.memory;

import com.hit.websocket.client.observability.model.ConnectionEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Thread-safe bounded event buffer with explicit lock ownership. */
final class RecentEventBuffer {

    private final Deque<ConnectionEvent> events = new ArrayDeque<>();
    private final int capacity;

    RecentEventBuffer(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    synchronized void add(ConnectionEvent event) {
        events.addFirst(event);
        while (events.size() > capacity) events.removeLast();
    }

    synchronized List<ConnectionEvent> snapshot(int limit) {
        return events.stream().limit(Math.clamp(limit, 1, capacity)).toList();
    }
}
