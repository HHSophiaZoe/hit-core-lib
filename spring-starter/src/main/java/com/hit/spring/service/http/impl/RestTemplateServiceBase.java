package com.hit.spring.service.http.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hit.spring.core.exception.HttpClientTimeoutException;
import com.hit.spring.core.exception.HttpResponseInvalidException;
import com.hit.spring.util.DataUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpMethod.*;

@Slf4j
@AllArgsConstructor
public abstract class RestTemplateServiceBase {

    private final RestTemplate template;

    private final ObjectMapper objectMapper;

    public String get(String url, HttpHeaders headers) {
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        return this.getEntity(url, headers, responseType).getBody();
    }

    public <R> R get(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.getEntity(url, headers, responseType).getBody();
    }

    public <R> ResponseEntity<R> getEntity(String url, HttpHeaders headers,
                                           ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, GET, headers, responseType);
    }

    public <B> String post(String url, B body, HttpHeaders headers) {
        ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        return this.postEntity(url, body, headers, responseType).getBody();
    }

    public <R> R post(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.postEntity(url, null, headers, responseType).getBody();
    }

    public <B, R> R post(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.postEntity(url, body, headers, responseType).getBody();
    }

    public <B, R> ResponseEntity<R> postEntity(String url, B body, HttpHeaders headers,
                                               ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, POST, body, headers, responseType);
    }

    public <B, R> R put(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.putEntity(url, body, headers, responseType).getBody();
    }

    public <B, R> ResponseEntity<R> putEntity(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.executeRequest(url, PUT, body, headers, responseType);
    }

    public<B, R> R patch(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType) {
        return this.patchEntity(url, body, headers, responseType).getBody();
    }

    public <B, R> ResponseEntity<R> patchEntity(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType)  {
        return this.executeRequest(url, PATCH, body, headers, responseType);
    }

    public <R> R delete(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType){
        return this.deleteEntity(url, headers, responseType).getBody();
    }

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
        HttpEntity<?> httpEntity;
        if (POST.equals(method) || PUT.equals(method) || PATCH.equals(method) || DELETE.equals(method)) {
            httpEntity = new HttpEntity<>(body, headers);
            log.info("Call api [{}]-[{}] \n\tBody: {} \n\tHeaders: {}", method, url, DataUtils.parserLog(body), headers.toString());
        } else {
            httpEntity = new HttpEntity<>(headers);
            log.info("Call api [{}]-[{}] \n\tHeaders: {}", method, url, headers.toString());
        }
        try {
            ResponseEntity<String> response = template.exchange(url, method, httpEntity, String.class);
            log.info("Call api [{}]-[{}] \n\tResponse: {}", method, url, response.getBody());

            if (responseType.getType().equals(String.class)) {
                return (ResponseEntity<R>) response;
            }

            JavaType javaType = objectMapper.getTypeFactory().constructType(responseType.getType());
            R data = objectMapper.readValue(response.getBody(), javaType);
            return new ResponseEntity<>(data, response.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Call api timeout [{}]-[{}]", method, url, e);
            throw new HttpClientTimeoutException(e.getMessage(), e);
        } catch (HttpStatusCodeException e) {
            log.error("Call api error [{}]-[{}]-[{}]: {}", method, url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Call api response error [{}]-[{}]", method, url, e);
            throw new HttpResponseInvalidException(e.getMessage());
        } catch (Exception e) {
            log.error("Call api error [{}]-[{}]: {}", method, url, e.getMessage(), e);
            throw e;
        }
    }

}