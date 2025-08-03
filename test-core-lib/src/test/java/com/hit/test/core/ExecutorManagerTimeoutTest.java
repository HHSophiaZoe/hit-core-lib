package com.hit.test.core;

import com.hit.spring.SpringStarterConfig;
import com.hit.spring.context.TrackingContext;
import com.hit.spring.core.manager.ExecutorManager;
import com.hit.spring.util.ThreadUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootTest(classes = SpringStarterConfig.class)
@TestPropertySource(properties = {
        "spring.threads.virtual.enabled=true",
        "app.task.executor.enable=true",
        "app.task.executor.thread-name-prefix=unit-test-",
        "app.task.executor.task-timeout-seconds=3"
})
public class ExecutorManagerTimeoutTest {

    @Autowired
    private ExecutorManager executorManager;

    private static final String CORRELATION_ID_DEFAULT = "unit-test";

    @AfterEach
    public void cleanup() {
        TrackingContext.clearTrackingContext();
        log.info("[com.hit.test.core.ExecutorManagerTimeoutTest] cleanup");
    }

    @Test
    @SneakyThrows
    public void testZipTasksFailureStrategyDEFAULT_Timeout() {
        long startTime = System.currentTimeMillis();
        try {
            executorManager.zipTasks(
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(1));
                        log.info("[testZipTasksFailureStrategyDEFAULT] Step one");
                    },
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(12));
                        log.info("[testZipTasksFailureStrategyDEFAULT] Step two");
                        throw new RuntimeException("Step two exception");
                    }
            );
        } catch (Exception e) {
            log.error("[testZipTasksFailureStrategyDEFAULT] Exception: {}", e.getMessage());
        }
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[testZipTasksFailureStrategyDEFAULT] Total time step: {}", totalTime);

        Assertions.assertTrue(totalTime >= 3000 && totalTime <= 3100);
    }

    @Test
    public void testZipTasksFailureStrategyERROR_CANCEL_WithTaskTimeout3Seconds() {
        long startTime = System.currentTimeMillis();
        AtomicReference<String> CORRELATION_ID_1 = new AtomicReference<>();
        AtomicReference<String> CORRELATION_ID_2 = new AtomicReference<>();
        AtomicReference<String> CORRELATION_ID_3 = new AtomicReference<>();
        try {
            TrackingContext.setCorrelationId(CORRELATION_ID_DEFAULT);
            log.info("[testZipTasksFailureStrategyERROR_CANCEL] CORRELATION_ID: {}", CORRELATION_ID_DEFAULT);
            executorManager.zipTasks(
                    ExecutorManager.FailureStrategy.ERROR_CANCEL,
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(2));
                        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Step one");
                        CORRELATION_ID_1.set(TrackingContext.getCorrelationId());
                    },
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(5));
                        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Step two");
                        CORRELATION_ID_2.set(TrackingContext.getCorrelationId());
                        throw new RuntimeException("Step two exception");
                    },
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(5));
                        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Step three");
                        CORRELATION_ID_3.set(TrackingContext.getCorrelationId());
                    }
            );
        } catch (Exception e) {
            log.error("[testZipTasksFailureStrategyERROR_CANCEL] Exception: {}", e.getMessage());
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Total time step: {}", totalTime);
        log.info("[testZipTasksFailureStrategyERROR_CANCEL] CORRELATION_ID_1: {}", CORRELATION_ID_1);
        log.info("[testZipTasksFailureStrategyERROR_CANCEL] CORRELATION_ID_2: {}", CORRELATION_ID_2);
        log.info("[testZipTasksFailureStrategyERROR_CANCEL] CORRELATION_ID_3: {}", CORRELATION_ID_3);

        Assertions.assertEquals(CORRELATION_ID_DEFAULT, CORRELATION_ID_1.get());
        Assertions.assertNull(CORRELATION_ID_2.get());
        Assertions.assertNull(CORRELATION_ID_3.get());
        Assertions.assertTrue(totalTime >= 3000 && totalTime <= 3100);
    }

}