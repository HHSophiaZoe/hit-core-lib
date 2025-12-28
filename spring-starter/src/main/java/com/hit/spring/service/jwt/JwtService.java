package com.hit.spring.service.jwt;

import com.hit.spring.config.properties.SecurityProperties;
import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.core.json.JsonMapper;
import com.hit.common.util.StringUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtService {

    private final SecurityProperties securityProperties;

    public static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String USER = "user";

    /**
     * Get SecretKey from configuration
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(securityProperties.getJwt().getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(SimpleSecurityUser simpleSecurityUser, Boolean isRefreshToken) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, BooleanUtils.isTrue(isRefreshToken) ? TYPE_REFRESH : TYPE_ACCESS);

        long currentTimeMillis = System.currentTimeMillis();

        if (BooleanUtils.isTrue(isRefreshToken)) {
            return Jwts.builder()
                    .claims(claims)
                    .issuedAt(new Date(currentTimeMillis))
                    .expiration(new Date(currentTimeMillis + (securityProperties.getJwt().getRefreshExpire() * 60 * 1000L)))
                    .signWith(this.getSecretKey())
                    .compact();
        }

        claims.put(USER, Base64.encodeBase64String(JsonMapper.encodeAsByte(simpleSecurityUser)));

        return Jwts.builder()
                .claims(claims)
                .subject(simpleSecurityUser.getId())
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + (securityProperties.getJwt().getAccessExpire() * 60 * 1000L)))
                .signWith(getSecretKey())
                .compact();
    }

    public SimpleSecurityUser extractToken(String token) {
        if (token == null || StringUtils.isBlank(token)) {
            return null;
        }

        try {
            Claims body = Jwts.parser()
                    .verifyWith(this.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userPrincipal = (String) body.get(USER);
            return JsonMapper.decodeValue(Base64.decodeBase64(userPrincipal), SimpleSecurityUser.class);
        } catch (Exception e) {
            log.error("extractToken ERROR: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("JWT security error: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT parse claims error", ex);
        }
        return false;
    }

}
