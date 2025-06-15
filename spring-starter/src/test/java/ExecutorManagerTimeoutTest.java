import com.hit.spring.SpringStarterConfig;
import com.hit.spring.core.manager.ExecutorManager;
import com.hit.spring.utils.ThreadUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static com.hit.spring.core.constants.enums.TrackingContextEnum.CORRELATION_ID;

@Slf4j
@SpringBootTest(classes = SpringStarterConfig.class)
@TestPropertySource(properties = {
        "spring.threads.virtual.enabled=true",
        "app.task.executor.enable=true",
        "app.task.executor.thread-name-prefix=unit-test-",
        "app.task.executor.task-timeout-seconds=10"
})
public class ExecutorManagerTimeoutTest {

    @Autowired
    private ExecutorManager executorManager;

    private static final String CORRELATION_ID_DEFAULT = "unit-test";

    @Test
    @SneakyThrows
    public void testZipTasksFailureStrategyDEFAULT_Timeout() {
        long startTime = System.currentTimeMillis();
        try {
            executorManager.zipTasks(
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(5));
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

        Assertions.assertTrue(totalTime >= 10000 && totalTime <= 10100);
    }

    @Test
    public void testZipTasksFailureStrategyERROR_CANCEL_WithTaskTimeout10Seconds() {
        long startTime = System.currentTimeMillis();
        AtomicReference<String> CORRELATION_ID_1 = new AtomicReference<>();
        AtomicReference<String> CORRELATION_ID_2 = new AtomicReference<>();
        AtomicReference<String> CORRELATION_ID_3 = new AtomicReference<>();
        try {
            ThreadContext.put(CORRELATION_ID.getKey(), CORRELATION_ID_DEFAULT);
            log.info("[testZipTasksFailureStrategyERROR_CANCEL] CORRELATION_ID: {}", CORRELATION_ID_DEFAULT);
            executorManager.zipTasks(
                    ExecutorManager.FailureStrategy.ERROR_CANCEL,
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(8));
                        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Step one");
                        CORRELATION_ID_1.set(ThreadContext.get(CORRELATION_ID.getKey()));
                    },
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(12));
                        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Step two");
                        CORRELATION_ID_2.set(ThreadContext.get(CORRELATION_ID.getKey()));
                        throw new RuntimeException("Step two exception");
                    },
                    () -> {
                        ThreadUtils.sleep(Duration.ofSeconds(12));
                        log.info("[testZipTasksFailureStrategyERROR_CANCEL] Step three");
                        CORRELATION_ID_3.set(ThreadContext.get(CORRELATION_ID.getKey()));
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
        Assertions.assertTrue(totalTime >= 10000 && totalTime <= 10100);
    }

}