package com.hit.spring.service.http.impl;

import com.hit.spring.core.exception.HttpClientTimeout;
import com.hit.spring.util.DataUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpMethod.*;

@Slf4j
@AllArgsConstructor
public abstract class RestTemplateServiceBase {

    private final RestTemplate template;

//
// =============================================== Non-Blocking ===================================================
//


//
// =============================================== Blocking ===================================================
//

    public String getBlocking(String url, HttpHeaders headers) {
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        return this.executeRequest(url, GET, headers, responseType).getBody();
    }

    public <R> R getBlocking(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, GET, headers, responseType).getBody();
    }

    public <R> ResponseEntity<R> getEntityBlocking(String url, HttpHeaders headers,
                                                   ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, GET, headers, responseType);
    }

    public <B> String postBlocking(String url, B body, HttpHeaders headers) {
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        return this.executeRequest(url, POST, body, headers, responseType).getBody();
    }

    public <B, R> R postBlocking(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, POST, body, headers, responseType).getBody();
    }

    public <B, R> ResponseEntity<R> postEntityBlocking(String url, B body, HttpHeaders headers,
                                                       ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, POST, body, headers, responseType);
    }

    private <R> ResponseEntity<R> executeRequest(String url, HttpMethod method, HttpHeaders headers,
                                                 ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, method, null, headers, responseType);
    }

    private <B, R> ResponseEntity<R> executeRequest(String url, HttpMethod method, B body,
                                                    HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        HttpEntity<?> httpEntity;
        if (POST.equals(method) || PUT.equals(method) || PATCH.equals(method) || DELETE.equals(method)) {
            httpEntity = new HttpEntity<>(body, headers);
        } else {
            httpEntity = new HttpEntity<>(headers);
        }
        try {
            log.debug("Call api [{}]-[{}] \n\tBody: {} \n\tHeaders: {}", method, url, DataUtils.parserLog(body), headers.toString());
            return template.exchange(url, method, httpEntity, responseType);
        } catch (ResourceAccessException e) {
            log.error("Call api timeout [{}]-[{}]", method, url);
            throw new HttpClientTimeout(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Call api error [{}]-[{}]: {}", method, url, e.getMessage());
            throw e;
        }
    }

}