import com.hit.spring.SpringStarterConfig;
import com.hit.spring.core.manager.ExecutorManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@SpringBootTest(classes = SpringStarterConfig.class)
@TestPropertySource(properties = {
        "app.task.executor.enable=true",
        "app.task.executor.thread-name-prefix=unit-test-",
})
public class ExecutorManagerTest {

    @Autowired
    private ExecutorManager executorManager;

    @Test
    public void testZipTasksReturn() {
        long startTime = System.currentTimeMillis();
        Integer result = executorManager.zipTasksReturn(
                () -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("[Unit Test] Step one = '1'");
                    return "1";
                },
                () -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("[Unit Test] Step two = 1");
                    return 1;
                },
                (a, b) -> Integer.parseInt(a) + b
        );

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("[Unit Test] Result total step: {}", result);
        log.info("[Unit Test] Total time step: {}", totalTime);

        Assertions.assertEquals(2, result);
        Assertions.assertTrue(totalTime >= 5000 && totalTime <= 6000);
    }
}