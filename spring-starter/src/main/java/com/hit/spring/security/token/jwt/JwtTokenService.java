package com.hit.spring.security.token.jwt;

import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.security.SecurityProperties;
import com.hit.spring.security.token.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.AbstractDeserializer;
import io.jsonwebtoken.io.AbstractSerializer;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * {@link TokenService} on HS256 JWTs signed with {@code app.security.jwt.secret-key}. The access token carries the
 * {@link SimpleSecurityUser} as Base64 JSON in the {@code user} claim. JSON goes through the application Jackson 3
 * mapper, so jjwt needs no JSON module of its own.
 */
@Slf4j
public class JwtTokenService implements TokenService {

    public static final String CLAIM_TYPE = "type";

    private static final String TYPE_ACCESS = "access";

    private static final String TYPE_REFRESH = "refresh";

    private static final String USER = "user";

    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    private final SecurityProperties securityProperties;

    private final JacksonClaimsSerializer serializer;

    private final JacksonClaimsDeserializer deserializer;

    public JwtTokenService(ObjectMapper objectMapper, SecurityProperties securityProperties) {
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
        this.serializer = new JacksonClaimsSerializer(objectMapper);
        this.deserializer = new JacksonClaimsDeserializer(objectMapper);
    }

    @Override
    public String generateAccessToken(SimpleSecurityUser user) {
        String encodedUser = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(user));
        return this.builder(TYPE_ACCESS, user, securityProperties.getJwt().getAccessExpire())
                .claim(USER, encodedUser)
                .compact();
    }

    @Override
    public String generateRefreshToken(SimpleSecurityUser user) {
        return this.builder(TYPE_REFRESH, user, securityProperties.getJwt().getRefreshExpire()).compact();
    }

    @Override
    public Optional<SimpleSecurityUser> readAccessToken(String token) {
        if (StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = this.parse(token);
            String user = claims.get(USER, String.class);
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class)) || user == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(Base64.getDecoder().decode(user), SimpleSecurityUser.class));
        } catch (Exception e) {
            log.warn("Unreadable access token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean isValid(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            this.parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private JwtBuilder builder(String type, SimpleSecurityUser user, long expireMinutes) {
        Instant now = Instant.now();
        return Jwts.builder()
                .json(serializer)
                .claim(CLAIM_TYPE, type)
                .subject(user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expireMinutes))))
                .signWith(this.getSecretKey());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .json(deserializer)
                .verifyWith(this.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Throws {@link io.jsonwebtoken.security.WeakKeyException} when the secret key is shorter than 256 bits.
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(securityProperties.getJwt().getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    private static final class JacksonClaimsSerializer extends AbstractSerializer<Map<String, ?>> {

        private final ObjectMapper objectMapper;

        private JacksonClaimsSerializer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected void doSerialize(Map<String, ?> claims, OutputStream out) {
            objectMapper.writeValue(out, claims);
        }
    }

    private static final class JacksonClaimsDeserializer extends AbstractDeserializer<Map<String, ?>> {

        private final ObjectMapper objectMapper;

        private JacksonClaimsDeserializer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected Map<String, ?> doDeserialize(Reader reader) {
            return objectMapper.readValue(reader, CLAIMS_TYPE);
        }
    }
}
