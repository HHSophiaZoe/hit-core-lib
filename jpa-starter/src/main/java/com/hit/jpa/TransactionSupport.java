package com.hit.jpa;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Runs a block in a transaction ({@code execute}) or in a new one ({@code executeOut}); rolls back on any exception.
 */
public class TransactionSupport {

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(Supplier<T> action) {
        return action.get();
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(Runnable action) {
        action.run();
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public <T> T executeOut(Supplier<T> action) {
        return action.get();
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void executeOut(Runnable action) {
        action.run();
    }

}
