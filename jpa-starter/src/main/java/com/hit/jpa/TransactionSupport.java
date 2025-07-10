package com.hit.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
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
