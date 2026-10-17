package com.hit.spring.security.token;

import com.hit.common.model.SimpleSecurityUser;

import java.util.Optional;

/**
 * Issues and reads the tokens that identify a caller, whatever their format. The starter provides a JWT
 * implementation; declare a bean of this type to use another one.
 */
public interface TokenService {

    String generateAccessToken(SimpleSecurityUser user);

    String generateRefreshToken(SimpleSecurityUser user);

    /**
     * The user of a valid access token; empty when the token is blank, invalid, expired or not an access token.
     */
    Optional<SimpleSecurityUser> readAccessToken(String token);

    /**
     * Whether the token, access or refresh, is valid and not expired.
     */
    boolean isValid(String token);
}
