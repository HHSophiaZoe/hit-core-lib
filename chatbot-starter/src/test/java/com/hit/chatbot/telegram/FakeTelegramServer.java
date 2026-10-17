package com.hit.chatbot.telegram;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bot API giả trên cổng ngẫu nhiên: trả lần lượt các response đã xếp hàng cho từng method và ghi lại request.
 */
class FakeTelegramServer implements AutoCloseable {

    record Request(String path, String body) {
    }

    private final HttpServer server;
    private final Map<String, ConcurrentLinkedQueue<String>> responses = new ConcurrentHashMap<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    FakeTelegramServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Request(path, body));
            String method = path.substring(path.lastIndexOf('/') + 1);
            String response = responses.getOrDefault(method, new ConcurrentLinkedQueue<>()).poll();
            if (response == null) {
                response = "{\"ok\":true,\"result\":[]}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void enqueue(String method, String responseJson) {
        responses.computeIfAbsent(method, key -> new ConcurrentLinkedQueue<>()).add(responseJson);
    }

    List<Request> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
