package com.hit.spring.security.token.jwt;

import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.security.SecurityProperties;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void accessTokenRoundTripsTheUser() {
        JwtTokenService tokenService = tokenService(SECRET, 60);
        SimpleSecurityUser user = SimpleSecurityUser.builder()
                .id("u-1").email("a@b.c").authorities(List.of("ADMIN")).build();

        String token = tokenService.generateAccessToken(user);

        assertThat(tokenService.isValid(token)).isTrue();
        assertThat(tokenService.readAccessToken(token)).hasValueSatisfying(extracted -> {
            assertThat(extracted.getId()).isEqualTo("u-1");
            assertThat(extracted.getEmail()).isEqualTo("a@b.c");
            assertThat(extracted.getAuthorities()).containsExactly("ADMIN");
        });
    }

    @Test
    void refreshTokenIsValidButIsNotAnAccessToken() {
        JwtTokenService tokenService = tokenService(SECRET, 60);

        String token = tokenService.generateRefreshToken(SimpleSecurityUser.initSystemAdmin());

        assertThat(tokenService.isValid(token)).isTrue();
        assertThat(tokenService.readAccessToken(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = tokenService("ffffffffffffffffffffffffffffffff", 60)
                .generateAccessToken(SimpleSecurityUser.initSystemAdmin());

        JwtTokenService tokenService = tokenService(SECRET, 60);
        assertThat(tokenService.isValid(token)).isFalse();
        assertThat(tokenService.readAccessToken(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenService tokenService = tokenService(SECRET, -1);

        String token = tokenService.generateAccessToken(SimpleSecurityUser.initSystemAdmin());

        assertThat(tokenService.isValid(token)).isFalse();
        assertThat(tokenService.readAccessToken(token)).isEmpty();
    }

    @Test
    void blankTokenIsRejected() {
        JwtTokenService tokenService = tokenService(SECRET, 60);

        assertThat(tokenService.isValid(" ")).isFalse();
        assertThat(tokenService.readAccessToken(null)).isEmpty();
    }

    @Test
    void secretShorterThan256BitsIsRefused() {
        JwtTokenService tokenService = tokenService("com.hit", 60);

        assertThatThrownBy(() -> tokenService.generateAccessToken(SimpleSecurityUser.initSystemAdmin()))
                .isInstanceOf(WeakKeyException.class);
    }

    private static JwtTokenService tokenService(String secret, int accessExpireMinutes) {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecretKey(secret);
        properties.getJwt().setAccessExpire(accessExpireMinutes);
        return new JwtTokenService(JsonMapper.builder().build(), properties);
    }
}
