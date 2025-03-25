package com.hit.spring.service.http;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public interface HttpService {

    String getBlocking(String url, HttpHeaders headers);

    <R> R getBlocking(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

    <R> ResponseEntity<R> getEntityBlocking(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

    <B> String postBlocking(String url, B body, HttpHeaders headers);

    <B, R> R postBlocking(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

    <B, R> ResponseEntity<R> postEntityBlocking(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

}