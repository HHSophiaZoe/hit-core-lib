package com.hit.test.virtual_thread;

import com.hit.jpa.properties.DataSourceDefaultProperties;
import com.hit.spring.SpringStarterConfig;
import com.hit.spring.config.embedded.EmbeddedTomcatConfig;
import com.hit.spring.config.http.DefaultRestTemplateConfig;
import com.hit.spring.config.properties.DefaultHttpClientProperties;
import com.hit.spring.core.filter.RequestLoggingFilter;
import com.hit.spring.service.http.HttpService;
import com.hit.common.util.ThreadUtils;
import com.hit.common.util.TraceUtils;
import com.hit.test.TestStarterConfig;
import com.hit.test.entity.TestEntity;
import com.hit.test.repository.TestRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@SpringBootTest(
        classes = {
                TestStarterConfig.class,
                SpringStarterConfig.class,
                EmbeddedTomcatConfig.class,
                RequestLoggingFilter.class,
                DefaultHttpClientProperties.class,
                DefaultRestTemplateConfig.class,
                HttpService.class,
                DataSourceDefaultProperties.class
        },
        properties = {
                "spring.threads.virtual.enabled=true",
                "app.enable-log-request-http=true",
                "http-client.default.enable=true",
//                "http-client.default.connection-pool.default-max-per-route=10000",
//                "http-client.default.connection-pool.max-total=10000",
                "logging.level.com=INFO",
                "logging.level.org=INFO",
                "logging.level.com.zaxxer.hikari=DEBUG"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class VirtualThreadIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private HttpService httpService;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private HikariDataSource dataSource;

    @Test
    void testApplicationStartup() {
        log.info("Application started successfully on port: {}", port);
        Assertions.assertTrue(port > 0);
    }

    @Test
    public void testVirtualThreadYielding() {
        // Test với platform threads (baseline)
        log.info("=== PLATFORM THREADS ===");
        testWithExecutor(Executors.newFixedThreadPool(1000), "Platform");

        // Test với virtual threads
        log.info("\n=== VIRTUAL THREADS ===");
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
        Map<String, Object> response = httpService.get(url, new HttpHeaders(), new ParameterizedTypeReference<>() {
        });
        log.info("Integration test response: {}", response);

        Assertions.assertEquals(Boolean.TRUE, response.get("isVirtual"));
        Assertions.assertTrue(response.get("threadGroup").toString().contains("VirtualThread"));
    }

    @Test
    void giveApiWhenCallApiThenUsingVirtualThreadV2() {
        ThreadUtils.sleep(Duration.ofSeconds(5));
        log.info("Actual HikariCP pool size: {}", dataSource.getMaximumPoolSize());
        log.info("Pool name: {}", dataSource.getPoolName());
        log.info("JDBC URL: {}", dataSource.getJdbcUrl());

        int threadCount = 1000; // Số thread cố định cho cả hai pool
        int numTasks = 20000;

        // Test với platform threads (baseline)
//        log.info("=== PLATFORM THREADS IO ===");
//        long startTimePlatform = System.currentTimeMillis();
//        Integer totalSuccessPlatform = testIOWithExecutor(Executors.newFixedThreadPool(threadCount), numTasks);
//        long totalTimePlatform = System.currentTimeMillis() - startTimePlatform;
//        log.info("Platform total execution time: {} ms - success: {}/{}", totalTimePlatform, totalSuccessPlatform, numTasks);

        // Test với virtual threads
        log.info("\n=== VIRTUAL THREADS IO ===");
        long startTimeVirtual = System.currentTimeMillis();
        Integer totalSuccessVirtual = testIOWithExecutor(Executors.newFixedThreadPool(threadCount, Thread.ofVirtual().factory()), numTasks);
        long totalTimeVirtual = System.currentTimeMillis() - startTimeVirtual;
        log.info("Virtual total execution time: {} ms - success: {}/{}", totalTimeVirtual, totalSuccessVirtual, numTasks);
    }

    private Integer testIOWithExecutor(ExecutorService executor, int numTasks) {
        CountDownLatch latch = new CountDownLatch(numTasks);
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < numTasks; i++) {
            executor.submit(() -> {
                try {
//                    testIOApi();
                    testIODb();
                    counter.incrementAndGet();
                } finally {
                    latch.countDown(); // Giảm latch khi task hoàn thành
                }
            });
        }

        try {
            latch.await(); // Chờ tất cả task hoàn thành
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return counter.get();
    }

    private void testIOApi() {
        String url = "http://localhost:" + port + "/api/benchmark/thread-info?order=" + TraceUtils.generateTraceId();
        Map<String, Object> response = httpService.get(url, new HttpHeaders(), new ParameterizedTypeReference<>() {
        });
        log.info("testIOApi response: {}", response);
    }

    private final Semaphore dbSemaphore = new Semaphore(10);

    public <T> T executeWithLimit(Supplier<T> operation) {
        try {
            try {
                dbSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return operation.get();
        } finally {
            dbSemaphore.release();
        }
    }

    private void testIODb() {
        executeWithLimit(() -> {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            log.warn("DB call - Waiting threads: {}, Active: {}, Total: {}",
                    pool.getThreadsAwaitingConnection(),
                    pool.getActiveConnections(),
                    pool.getTotalConnections());

            return testRepository.save(TestEntity.builder()
                    .code(TraceUtils.generateTraceId())
                    .name(TraceUtils.generateTraceId())
                    .build());
        });
    }

}
