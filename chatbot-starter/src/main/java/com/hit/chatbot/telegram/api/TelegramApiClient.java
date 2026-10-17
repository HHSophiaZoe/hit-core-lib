package com.hit.chatbot.telegram.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client tối giản cho Telegram Bot API: chỉ {@code getUpdates} (long polling) và {@code sendMessage}.
 */
public class TelegramApiClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT_MARGIN = Duration.ofSeconds(10);

    private static final int TOO_MANY_REQUESTS = 429;

    private final String methodBaseUrl;
    private final Duration maxRetryWait;
    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final JavaType updatesResponseType;
    private final JavaType messageResponseType;

    public TelegramApiClient(String baseUrl, String token) {
        this(baseUrl, token, Duration.ofSeconds(30));
    }

    /**
     * @param maxRetryWait longest wait before sending again a message Telegram refused for rate limiting; a longer
     *                     wait fails the send instead
     */
    public TelegramApiClient(String baseUrl, String token, Duration maxRetryWait) {
        this.methodBaseUrl = baseUrl + "/bot" + token + "/";
        this.maxRetryWait = maxRetryWait;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.jsonMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        this.updatesResponseType = jsonMapper.getTypeFactory().constructParametricType(TelegramResponse.class,
                jsonMapper.getTypeFactory().constructCollectionType(List.class, TelegramUpdate.class));
        this.messageResponseType = jsonMapper.getTypeFactory().constructParametricType(TelegramResponse.class, TelegramMessage.class);
    }

    /**
     * Chờ tối đa {@code timeout} để có update mới; {@code offset} là updateId kế tiếp cần nhận.
     */
    public List<TelegramUpdate> getUpdates(long offset, Duration timeout) {
        Map<String, Object> body = Map.of(
                "offset", offset,
                "timeout", timeout.toSeconds(),
                "allowed_updates", List.of("message"));
        TelegramResponse<List<TelegramUpdate>> response = this.call("getUpdates", body, timeout.plus(REQUEST_TIMEOUT_MARGIN), updatesResponseType);
        return response.result() != null ? response.result() : List.of();
    }

    /**
     * Sends a message; when Telegram refuses it for rate limiting (429), waits the time it asks, up to
     * {@code maxRetryWait}, and sends it once more.
     */
    public TelegramMessage sendMessage(TelegramSendMessage message) {
        try {
            return this.callSendMessage(message);
        } catch (TelegramApiException e) {
            Duration retryAfter = e.getRetryAfter();
            if (retryAfter == null || retryAfter.compareTo(maxRetryWait) > 0) {
                throw e;
            }
            try {
                Thread.sleep(retryAfter);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw e;
            }
            return this.callSendMessage(message);
        }
    }

    private TelegramMessage callSendMessage(TelegramSendMessage message) {
        return this.<TelegramMessage>call("sendMessage", message, REQUEST_TIMEOUT_MARGIN.multipliedBy(3), messageResponseType).result();
    }

    private <T> TelegramResponse<T> call(String method, Object body, Duration timeout, JavaType responseType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(methodBaseUrl + method))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(jsonMapper.writeValueAsBytes(body)))
                .build();
        HttpResponse<byte[]> httpResponse;
        try {
            httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new TelegramApiException("Telegram " + method + " failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramApiException("Telegram " + method + " interrupted", e);
        }

        TelegramResponse<T> response = jsonMapper.readValue(httpResponse.body(), responseType);
        if (!response.ok()) {
            Duration retryAfter = response.errorCode() != null && response.errorCode() == TOO_MANY_REQUESTS
                    && response.parameters() != null && response.parameters().retryAfter() != null
                    ? Duration.ofSeconds(response.parameters().retryAfter()) : null;
            throw new TelegramApiException("Telegram " + method + " rejected: " + response.errorCode() + " "
                    + response.description(), retryAfter);
        }
        return response;
    }
}
