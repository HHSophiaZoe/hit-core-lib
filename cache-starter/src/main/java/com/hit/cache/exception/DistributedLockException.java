package com.hit.cache.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
public class DistributedLockException extends RuntimeException {

    public DistributedLockException() {
    }

    public DistributedLockException(String key) {
        super("DistributedLock: cannot wait for key " + key);
    }

    public static class None extends DistributedLockException {
        @Serial
        private static final long serialVersionUID = 1L;

        private None() {
            super();
        }
    }
}
