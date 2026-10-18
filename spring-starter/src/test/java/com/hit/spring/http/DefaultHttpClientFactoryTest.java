package com.hit.spring.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientResponseException;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultHttpClientFactoryTest {

    @ParameterizedTest
    @CsvSource({"POST,503", "PUT,503", "GET,503", "DELETE,503",
            "POST,429", "PUT,429", "GET,429", "DELETE,429"})
    void returnsOriginalErrorWithoutResubmittingRequest(String method, int status) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/order", exchange -> {
            exchange.getRequestBody().readAllBytes();
            // A hidden retry would turn the original error into an apparent success.
            exchange.sendResponseHeaders(requests.incrementAndGet() == 1 ? status : 200, -1);
            exchange.close();
        });
        server.start();
        DefaultHttpClientFactory factory = new DefaultHttpClientFactory(new DefaultHttpClientProperties());
        var requestFactory = factory.createRequestFactory();
        try {
            var client = factory.createRestClient(requestFactory);
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/order";

            RestClientResponseException error = assertThrows(RestClientResponseException.class,
                    () -> client.method(HttpMethod.valueOf(method)).uri(url).retrieve().toBodilessEntity());

            assertEquals(status, error.getStatusCode().value());
            assertEquals(1, requests.get());
        } finally {
            requestFactory.destroy();
            server.stop(0);
        }
    }
}
