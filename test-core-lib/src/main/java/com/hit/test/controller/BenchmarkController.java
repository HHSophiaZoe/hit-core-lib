package com.hit.test.controller;

import com.hit.spring.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Profile("test")
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    @GetMapping("/thread-info")
    public ResponseEntity<Map<String, Object>> benchmark(@RequestParam String order) {
        Thread currentThread = Thread.currentThread();
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("order", order);
        threadInfo.put("threadName", currentThread.getName());
        threadInfo.put("threadId", currentThread.threadId());
        threadInfo.put("isVirtual", currentThread.isVirtual());
        threadInfo.put("threadGroup", currentThread.getThreadGroup().getName());
        threadInfo.put("state", currentThread.getState().toString());
        threadInfo.put("priority", currentThread.getPriority());
        threadInfo.put("timestamp", Instant.now());
        ThreadUtils.sleep(Duration.ofSeconds(2));
        return ResponseEntity.ok(threadInfo);
    }
}