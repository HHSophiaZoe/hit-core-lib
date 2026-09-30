package com.hit.spring.core.exception;

import com.hit.common.model.ResponseStatusCode;
import org.springframework.http.HttpStatus;

public interface ResponseStatusCodeEnum {
    ResponseStatusCode SUCCESS = code("SUCCESS", HttpStatus.OK);
    ResponseStatusCode INTERNAL_GENERAL_SERVER_ERROR = code("GENERAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    ResponseStatusCode BUSINESS_ERROR = code("BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    ResponseStatusCode VALIDATION_ERROR = code("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    ResponseStatusCode RESOURCE_NOT_FOUND = code("RESOURCE_NOT_FOUND", HttpStatus.BAD_REQUEST);
    ResponseStatusCode SHOW_RESOURCES_NOT_FOUND = code("SHOW_RESOURCES_NOT_FOUND", HttpStatus.BAD_REQUEST);

    // Auth
    ResponseStatusCode UNAUTHORIZED_ERROR = code("AUTH001", HttpStatus.UNAUTHORIZED);
    ResponseStatusCode FORBIDDEN_ERROR = code("AUTH002", HttpStatus.FORBIDDEN);
    ResponseStatusCode NOT_PERMISSION_DELETE_UPDATE = code("AUTH003", HttpStatus.FORBIDDEN);
    ResponseStatusCode INCORRECT_EMAIL = code("AUTH004", HttpStatus.BAD_REQUEST);
    ResponseStatusCode INCORRECT_EMAIL_OR_PHONE = code("AUTH005", HttpStatus.BAD_REQUEST);
    ResponseStatusCode PASSWORD_INCORRECT = code("AUTH006", HttpStatus.BAD_REQUEST);
    ResponseStatusCode ACCOUNT_LOCKED = code("AUTH007", HttpStatus.LOCKED);
    ResponseStatusCode ACCOUNT_NOT_ENABLED = code("AUTH008", HttpStatus.LOCKED);
    ResponseStatusCode INVALID_TOKEN = code("AUTH009", HttpStatus.BAD_REQUEST);
    ResponseStatusCode EXPIRED_TOKEN = code("AUTH010", HttpStatus.BAD_REQUEST);
    ResponseStatusCode INVALID_REFRESH_TOKEN = code("AUTH011", HttpStatus.BAD_REQUEST);
    ResponseStatusCode EMAIL_OR_PHONE_REGISTERED = code("AUTH012", HttpStatus.BAD_REQUEST);
    ResponseStatusCode NEW_PASSWORD_MATCHES_OLD_PASSWORD = code("AUTH013", HttpStatus.BAD_REQUEST);

    private static ResponseStatusCode code(String value, HttpStatus status) {
        return ResponseStatusCode.builder().code(value).httpStatus(status).build();
    }
}
