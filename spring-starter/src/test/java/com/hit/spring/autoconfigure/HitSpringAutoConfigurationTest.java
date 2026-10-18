package com.hit.spring.autoconfigure;

import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.util.BeanUtils;
import com.hit.spring.util.EnvironmentUtils;
import com.hit.spring.security.token.TokenService;
import com.hit.spring.security.token.jwt.JwtTokenService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HitSpringAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, HitSpringAutoConfiguration.class));

    @Test
    void jwtIsTheDefaultTokenService() {
        runner.withPropertyValues("app.security.jwt.secret-key=test-secret").run(context -> {
            assertThat(context.getBeanFactory().containsSingleton("tokenService")).isFalse();
            assertThat(context).getBean(TokenService.class).isInstanceOf(JwtTokenService.class);
        });
    }

    @Test
    void applicationTokenServiceReplacesTheJwtOne() {
        runner.withPropertyValues("app.security.jwt.secret-key=test-secret")
                .withBean(TokenService.class, StubTokenService::new)
                .run(context -> assertThat(context).getBean(TokenService.class).isInstanceOf(StubTokenService.class));
    }

    @Test
    void noTokenServiceWithoutJjwt() {
        runner.withPropertyValues("app.security.jwt.secret-key=test-secret")
                .withClassLoader(new FilteredClassLoader(Jwts.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(TokenService.class));
    }

    @Test
    void noJwtWithoutConfiguration() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(TokenService.class));
    }

    @Test
    void staticContextHoldersWorkWithoutBeingInjected() {
        runner.withPropertyValues("audit.marker=ready").run(context -> {
            assertThat(context.getBeanFactory().containsSingleton("hitBeanUtils")).isTrue();
            assertThat(context.getBeanFactory().containsSingleton("hitEnvironmentUtils")).isTrue();
            assertThat(EnvironmentUtils.get("audit.marker")).isEqualTo("ready");
            assertThat(BeanUtils.getBean("messageSource", Object.class)).isSameAs(context.getBean("messageSource"));
        });
    }

    static class StubTokenService implements TokenService {

        @Override
        public String generateAccessToken(SimpleSecurityUser user) {
            return "access";
        }

        @Override
        public String generateRefreshToken(SimpleSecurityUser user) {
            return "refresh";
        }

        @Override
        public Optional<SimpleSecurityUser> readAccessToken(String token) {
            return Optional.empty();
        }

        @Override
        public boolean isValid(String token) {
            return false;
        }
    }
}
