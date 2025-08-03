package com.hit.test.virtual_thread;

import com.hit.spring.SpringStarterConfig;
import com.hit.spring.config.embedded.EmbeddedTomcatConfig;
import com.hit.spring.core.filter.RequestLoggingFilter;
import com.hit.spring.util.ThreadUtils;
import com.hit.spring.util.TraceUtils;
import com.hit.test.TestStarterConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@SpringBootTest(
        classes = {
                TestStarterConfig.class,
                SpringStarterConfig.class,
                EmbeddedTomcatConfig.class,
                RequestLoggingFilter.class
        },
        properties = {
                "spring.threads.virtual.enabled=true",
                "app.enable-log-request-http=true",
                "logging.level.com=INFO",
                "logging.level.org=INFO"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class VirtualThreadIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testApplicationStartup() {
        log.info("Application started successfully on port: {}", port);
        Assertions.assertTrue(port > 0);
    }

    @Test
    public void testVirtualThreadYielding() {
        // Test với platform threads (baseline)
        System.out.println("=== PLATFORM THREADS ===");
        testWithExecutor(Executors.newFixedThreadPool(1000), "Platform");

        // Test với virtual threads
        System.out.println("\n=== VIRTUAL THREADS ===");
        testWithExecutor(new VirtualThreadExecutor("unit-test-"), "Virtual");
    }

    private void testWithExecutor(ExecutorService executor, String type) {
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                log.info("[{}] Task {} started on thread: {} (isVirtual: {})",
                        type, taskId, Thread.currentThread().getName(), Thread.currentThread().isVirtual());

                ThreadUtils.sleep(Duration.ofMillis(1000));
                log.info("[{}] Task {} completed on thread: {}", type, taskId, Thread.currentThread().getName());
                latch.countDown();
            });
        }

        try {
            latch.await();
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[{}] Total execution time: {} ms", type, totalTime);

            if (type.equals("Platform")) {
                log.info("Expected time with 2 platform threads: ~6000ms (3 batches)%n");
            } else {
                log.info("Expected time with virtual threads: ~2000ms (all parallel)%n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
    }

    @Test
    void giveApiWhenCallApiThenUsingVirtualThread() {
        String url = "http://localhost:" + port + "/api/benchmark/thread-info?order=" + TraceUtils.generateTraceId();
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        log.info("Integration test response: {}", response.getBody());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(Boolean.TRUE, response.getBody().get("isVirtual"));
        Assertions.assertTrue(response.getBody().get("threadGroup").toString().contains("VirtualThread"));
    }

    @Test
    void giveApiWhenCallApiThenUsingVirtualThreadV2() {
        Callable<Void> task = () -> {
            String url = "http://localhost:" + port + "/api/benchmark/thread-info?order=" + TraceUtils.generateTraceId();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            log.info("Response: {}", response.getBody());

            Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
            Assertions.assertNotNull(response.getBody());
            Assertions.assertEquals(Boolean.TRUE, response.getBody().get("isVirtual"));
            Assertions.assertTrue(response.getBody().get("threadGroup").toString().contains("VirtualThread"));

            return null;
        };

        int threadCount = 100;
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }

        // Đợi tất cả gọi xong
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

}
