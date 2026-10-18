package com.hit.spring.http;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpMethod.*;

/**
 * {@link HttpService} over a {@link RestClient}: the body is read as a String, logged, then parsed with the application
 * {@link ObjectMapper}. Create one per {@link RestClient} to call an API with other timeouts or headers.
 */
@Slf4j
@AllArgsConstructor
public class RestClientHttpService implements HttpService {

    private static final int MAX_RESPONSE_LOG_LENGTH = 2048;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    @Override
    public String get(String url, HttpHeaders headers) {
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        return this.getEntity(url, headers, responseType).getBody();
    }

    @Override
    public <R> R get(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.getEntity(url, headers, responseType).getBody();
    }

    @Override
    public <R> ResponseEntity<R> getEntity(String url, HttpHeaders headers,
                                           ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, GET, headers, responseType);
    }

    @Override
    public <B> String post(String url, B body, HttpHeaders headers) {
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        return this.postEntity(url, body, headers, responseType).getBody();
    }

    @Override
    public <R> R post(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.postEntity(url, null, headers, responseType).getBody();
    }

    @Override
    public <B, R> R post(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.postEntity(url, body, headers, responseType).getBody();
    }

    @Override
    public <B, R> ResponseEntity<R> postEntity(String url, B body, HttpHeaders headers,
                                               ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, POST, body, headers, responseType);
    }

    @Override
    public <B, R> R put(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.putEntity(url, body, headers, responseType).getBody();
    }

    @Override
    public <B, R> ResponseEntity<R> putEntity(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, PUT, body, headers, responseType);
    }

    @Override
    public <B, R> R patch(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.patchEntity(url, body, headers, responseType).getBody();
    }

    @Override
    public <B, R> ResponseEntity<R> patchEntity(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, PATCH, body, headers, responseType);
    }

    @Override
    public <R> R delete(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.deleteEntity(url, headers, responseType).getBody();
    }

    @Override
    public <R> ResponseEntity<R> deleteEntity(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, DELETE, headers, responseType);
    }

    protected <R> ResponseEntity<R> executeRequest(String url, HttpMethod method, HttpHeaders headers,
                                                   ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, method, null, headers, responseType);
    }

    @SuppressWarnings({"unchecked"})
    protected <B, R> ResponseEntity<R> executeRequest(String url, HttpMethod method, B body,
                                                      HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        boolean hasBody = POST.equals(method) || PUT.equals(method) || PATCH.equals(method) || DELETE.equals(method);
        if (hasBody) {
            log.info("Call api [{}]-[{}] \n\tBody: {}", method, url, body);
        } else {
            log.info("Call api [{}]-[{}]", method, url);
        }
        try {
            RestClient.RequestBodySpec request = restClient.method(method)
                    .uri(url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers));
            if (hasBody && body != null) {
                request.body(body);
            }
            ResponseEntity<String> response = request.retrieve().toEntity(String.class);
            log.info("Call api [{}]-[{}] \n\tResponse: {}", method, url, truncate(response.getBody()));

            if (responseType.getType().equals(String.class)) {
                return (ResponseEntity<R>) response;
            }

            JavaType javaType = objectMapper.getTypeFactory().constructType(responseType.getType());
            R data = objectMapper.readValue(response.getBody(), javaType);
            return new ResponseEntity<>(data, response.getHeaders(), response.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Call api timeout [{}]-[{}]", method, url);
            throw new HttpClientTimeoutException(e.getMessage(), e);
        } catch (HttpStatusCodeException e) {
            log.error("Call api error response [{}]-[{}] [{}]: {}", method, url, e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            if (HttpStatus.GATEWAY_TIMEOUT.equals(e.getStatusCode())) {
                throw new HttpClientTimeoutException(e.getMessage(), e);
            }
            throw e;
        } catch (JacksonException e) {
            log.error("Call api response error [{}]-[{}]", method, url, e);
            throw new HttpResponseInvalidException(e.getMessage());
        } catch (Exception e) {
            log.error("Call api error [{}]-[{}]: {}", method, url, e.getMessage(), e);
            throw e;
        }
    }

    private static String truncate(String body) {
        return body == null || body.length() <= MAX_RESPONSE_LOG_LENGTH ? body
                : body.substring(0, MAX_RESPONSE_LOG_LENGTH) + "...[truncated]";
    }

}