package com.hit.spring.web;

import com.hit.spring.exception.BaseResponseException;
import com.hit.spring.exception.BusinessException;
import com.hit.spring.exception.ResponseStatusCodeEnum;
import com.hit.common.model.ResponseStatus;
import com.hit.spring.web.response.GeneralResponse;
import com.hit.spring.web.response.ResponseFactory;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into {@link GeneralResponse} errors. Application advices are consulted first, so a service may
 * handle its own exceptions; this one is the fallback.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GeneralResponse<Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ResponseStatusCodeEnum.RESOURCE_NOT_FOUND.code(), ex.getMessage());
    }

    // Validation of @Validated method parameters
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GeneralResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> result = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = null;
            for (Path.Node node : violation.getPropertyPath()) {
                fieldName = node.getName();
            }
            result.put(fieldName, violation.getMessage());
        });
        return validationError(result);
    }

    // Validation of @Valid request bodies and bound model attributes
    @ExceptionHandler(BindException.class)
    public ResponseEntity<GeneralResponse<Object>> handleBindException(BindException ex) {
        Map<String, String> result = new LinkedHashMap<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String name = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            result.put(name, error.getDefaultMessage());
        }
        return validationError(result);
    }

    // Built-in validation of controller method parameters (constraints on @PathVariable, @RequestParam, ...)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GeneralResponse<Object>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        Map<String, String> result = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(parameterResult -> {
            String name = parameterResult.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : parameterResult.getResolvableErrors()) {
                result.put(name, error.getDefaultMessage());
            }
        });
        return validationError(result);
    }

    // Path variable or request parameter that cannot be converted, e.g. a malformed UUID
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GeneralResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "value";
        return validationError(Map.of(ex.getName(), "must be a valid " + requiredType));
    }

    // Missing or malformed request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GeneralResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ResponseStatusCodeEnum.VALIDATION_ERROR.code(), "Request body is missing or malformed");
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<GeneralResponse<Object>> handleServletRequestBindingException(ServletRequestBindingException ex) {
        return error(HttpStatus.BAD_REQUEST, ResponseStatusCodeEnum.VALIDATION_ERROR.code(), ex.getMessage());
    }

    @ExceptionHandler(BaseResponseException.class)
    public ResponseEntity<GeneralResponse<Object>> handleBaseResponseException(BaseResponseException ex) {
        if (ex.getResponseStatus() != null) {
            return ResponseFactory.error(ex.getResponseStatus());
        }
        return ResponseFactory.error(ex.getResponseStatusCode(), ex.getParams());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GeneralResponse<Object>> handleBusinessException(BusinessException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseFactory.error(ResponseStatusCodeEnum.BUSINESS_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponse<Object>> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResponseFactory.error(ResponseStatusCodeEnum.INTERNAL_GENERAL_SERVER_ERROR);
    }

    private static ResponseEntity<GeneralResponse<Object>> validationError(Map<String, String> fields) {
        return error(HttpStatus.BAD_REQUEST, ResponseStatusCodeEnum.VALIDATION_ERROR.code(), fields);
    }

    private static ResponseEntity<GeneralResponse<Object>> error(HttpStatus httpStatus, String code, Object message) {
        GeneralResponse<Object> response = new GeneralResponse<>();
        response.setStatus(new ResponseStatus(code, message));
        return ResponseEntity.status(httpStatus).body(response);
    }
}
