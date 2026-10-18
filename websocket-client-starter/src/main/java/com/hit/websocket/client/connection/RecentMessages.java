package com.hit.websocket.client.connection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Nhớ hash 64-bit của message trong một khoảng thời gian để nhận ra message trùng nội dung. */
final class RecentMessages {

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private final long windowNanos;
    private final Deque<long[]> entries = new ArrayDeque<>(); // {hash, nhận lúc (nanoTime)} theo thứ tự nhận.
    private final Set<Long> hashes = new HashSet<>();

    RecentMessages(long windowNanos) {
        this.windowNanos = windowNanos;
    }

    /** Trả về false nếu message đã gặp trong cửa sổ thời gian. */
    boolean add(String payload, long nowNanos) {
        evictOlderThan(nowNanos - windowNanos);
        long hash = hash(payload);
        if (!hashes.add(hash)) return false;
        entries.addLast(new long[]{hash, nowNanos});
        return true;
    }

    void clear() {
        entries.clear();
        hashes.clear();
    }

    private void evictOlderThan(long cutoffNanos) {
        while (!entries.isEmpty() && entries.peekFirst()[1] - cutoffNanos < 0) {
            hashes.remove(entries.pollFirst()[0]);
        }
    }

    // FNV-1a 64-bit: nhanh, đủ phân tán; xác suất trùng với vài trăm nghìn message trong cửa sổ là không đáng kể.
    private static long hash(String payload) {
        long hash = FNV_OFFSET;
        for (int i = 0; i < payload.length(); i++) {
            hash ^= payload.charAt(i);
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
