package com.hit.spring.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.json.JsonMapper;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gọi HttpService thật qua Apache HttpClient tới một server local, kiểm tra request gửi đi và cách xử lý response.
 */
class RestClientHttpServiceTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    record Request(String method, String rawUri, String accept, String contentType, String authorization, String body) {
    }

    record OrderBody(String symbol, int quantity) {
    }

    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            exchange.getRequestHeaders().forEach(headers::addAll);
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath() + "?"
                    + exchange.getRequestURI().getRawQuery(), headers.getFirst("Accept"),
                    headers.getFirst("Content-Type"), headers.getFirst("Authorization"), body));
            String path = exchange.getRequestURI().getPath();
            int status = switch (path) {
                case "/not-found" -> 404;
                case "/error" -> 500;
                case "/gateway-timeout" -> 504;
                default -> 200;
            };
            if (path.equals("/slow")) {
                sleep(Duration.ofSeconds(3));
            }
            String response = path.equals("/invalid") ? "{not json" : "{\"code\":\"" + path + "\",\"name\":\"Việt\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void getKeepsUrlAndSendsHeaders() {
        String query = "?from=2026-10-17T09:00:00+07:00&symbols=VN30F1M,VN30F2M&page=1";

        Map<String, Object> response = service(new DefaultHttpClientProperties()).get(baseUrl + "/bars" + query,
                headers(), MAP_TYPE);

        assertEquals(Map.of("code", "/bars", "name", "Việt"), response);
        Request request = requests.getFirst();
        assertEquals("GET", request.method());
        assertEquals("/bars" + query, request.rawUri());
        assertEquals("Bearer token", request.authorization());
        assertEquals("text/plain, application/json, application/yaml, application/*+json, */*", request.accept());
        assertEquals("", request.body());
    }

    @Test
    void callerAcceptHeaderOverridesDefault() {
        HttpHeaders headers = headers();
        headers.add("Accept", "application/json");

        service(new DefaultHttpClientProperties()).get(baseUrl + "/accept", headers);

        assertEquals("application/json", requests.getFirst().accept());
    }

    @Test
    void postSerializesBodyAsJson() {
        ResponseEntity<Map<String, Object>> response = service(new DefaultHttpClientProperties())
                .postEntity(baseUrl + "/orders", new OrderBody("VN30F1M", 2), headers(), MAP_TYPE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("/orders", response.getBody().get("code"));
        Request request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals("application/json", request.contentType());
        assertEquals("{\"symbol\":\"VN30F1M\",\"quantity\":2}", request.body());
    }

    @Test
    void postWithoutBodySendsEmptyBody() {
        service(new DefaultHttpClientProperties()).post(baseUrl + "/token", headers(), MAP_TYPE);

        Request request = requests.getFirst();
        assertEquals("POST", request.method());
        assertNull(request.contentType());
        assertEquals("", request.body());
    }

    @Test
    void putPatchDeleteUseTheirMethods() {
        HttpService service = service(new DefaultHttpClientProperties());

        service.put(baseUrl + "/a", new OrderBody("A", 1), headers(), MAP_TYPE);
        service.patch(baseUrl + "/b", new OrderBody("B", 2), headers(), MAP_TYPE);
        service.delete(baseUrl + "/c", headers(), MAP_TYPE);

        assertEquals(List.of("PUT", "PATCH", "DELETE"), requests.stream().map(Request::method).toList());
        assertEquals("{\"symbol\":\"B\",\"quantity\":2}", requests.get(1).body());
    }

    @Test
    void clientErrorKeepsStatusAndBody() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> service(new DefaultHttpClientProperties()).get(baseUrl + "/not-found", headers(), MAP_TYPE));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("{\"code\":\"/not-found\",\"name\":\"Việt\"}", exception.getResponseBodyAsString(StandardCharsets.UTF_8));
    }

    @Test
    void serverErrorIsRethrown() {
        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class,
                () -> service(new DefaultHttpClientProperties()).get(baseUrl + "/error", headers(), MAP_TYPE));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void gatewayTimeoutBecomesTimeoutException() {
        assertThrows(HttpClientTimeoutException.class,
                () -> service(new DefaultHttpClientProperties()).get(baseUrl + "/gateway-timeout", headers(), MAP_TYPE));
    }

    @Test
    void invalidJsonBecomesInvalidResponseException() {
        assertThrows(HttpResponseInvalidException.class,
                () -> service(new DefaultHttpClientProperties()).get(baseUrl + "/invalid", headers(), MAP_TYPE));
    }

    @Test
    void connectionRefusedBecomesTimeoutException() {
        assertThrows(HttpClientTimeoutException.class,
                () -> service(new DefaultHttpClientProperties()).get("http://127.0.0.1:9/closed", headers(), MAP_TYPE));
    }

    @Test
    void responseTimeoutBecomesTimeoutException() {
        DefaultHttpClientProperties properties = new DefaultHttpClientProperties();
        properties.getConnection().setResponseTimeout(1);

        assertThrows(HttpClientTimeoutException.class,
                () -> service(properties).get(baseUrl + "/slow", headers(), MAP_TYPE));
    }

    @Test
    void requestWaitsForPooledConnectionThenTimesOut() {
        DefaultHttpClientProperties properties = new DefaultHttpClientProperties();
        properties.getConnectionPool().setDefaultMaxPerRoute(1);
        properties.getConnection().setConnectRequestTimeout(1);
        HttpService service = service(properties);

        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> service.get(baseUrl + "/slow", headers()),
                Executors.newVirtualThreadPerTaskExecutor());
        sleep(Duration.ofMillis(500));

        assertThrows(HttpClientTimeoutException.class, () -> service.get(baseUrl + "/second", headers()));
        assertEquals("{\"code\":\"/slow\",\"name\":\"Việt\"}", first.join());
        assertEquals(List.of("/slow?null"), requests.stream().map(Request::rawUri).toList());
    }

    @Test
    void httpsTrustsSelfSignedCertificateWithOtherHostname() throws Exception {
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(selfSignedContext()));
        httpsServer.createContext("/", exchange -> {
            byte[] bytes = "{\"secure\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpsServer.start();
        try {
            Map<String, Object> response = service(new DefaultHttpClientProperties())
                    .get("https://127.0.0.1:" + httpsServer.getAddress().getPort() + "/tls", headers(), MAP_TYPE);

            assertEquals(Map.of("secure", true), response);
        } finally {
            httpsServer.stop(0);
        }
    }

    private static HttpService service(DefaultHttpClientProperties properties) {
        return new RestClientHttpService(new DefaultHttpClientFactory(properties).createRestClient(),
                JsonMapper.builder().build());
    }

    private static HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer token");
        return headers;
    }

    private static SSLContext selfSignedContext() throws Exception {
        char[] password = "changeit".toCharArray();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = RestClientHttpServiceTest.class.getResourceAsStream("/http/self-signed.p12")) {
            keyStore.load(inputStream, password);
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
