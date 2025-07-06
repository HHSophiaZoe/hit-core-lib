package com.hit.spring.service.http;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public interface HttpService {

    String get(String url, HttpHeaders headers);

    <R> R get(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

    <R> ResponseEntity<R> getEntity(String url, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

    <B> String post(String url, B body, HttpHeaders headers);

    <B, R> R post(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

    <B, R> ResponseEntity<R> postEntity(String url, B body, HttpHeaders headers, ParameterizedTypeReference<R> responseType);

}