package com.hit.cache.lock.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
public class DistributedLockException extends RuntimeException {

    public record DistributedLockData(String name, String value) {
    }

    private DistributedLockData data;

    public DistributedLockException() {
    }

    public DistributedLockException(DistributedLockData data) {
        super("DistributedLock: cannot wait for key " + data.name() + "::" + data.value());
        this.data = data;
    }

    public static class None extends DistributedLockException {
        @Serial
        private static final long serialVersionUID = 1L;

        private None() {
            super();
        }
    }
}
