package com.hit.spring.security.filter;

import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.i18n.Translator;
import com.hit.spring.security.SecurityProperties;
import com.hit.spring.security.SecurityContext;
import com.hit.spring.json.JsonMapper;
import com.hit.spring.security.token.jwt.JwtTokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private static final tools.jackson.databind.json.JsonMapper MAPPER = tools.jackson.databind.json.JsonMapper.builder().build();

    @BeforeAll
    static void initStaticHolders() {
        new JsonMapper(MAPPER);
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        new Translator().setMessageSource(messageSource);
    }

    private final SecurityProperties properties = securityProperties();

    private final JwtTokenService tokenService = new JwtTokenService(MAPPER, properties);

    private final AuthenticationFilter filter = new AuthenticationFilter(tokenService, properties);

    @Test
    void bearerTokenPutsTheUserInContextAndClearsItAfterwards() throws Exception {
        String token = tokenService.generateAccessToken(SimpleSecurityUser.builder().id("u-1").build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");
        request.addHeader("Authorization", "Bearer " + token);
        AtomicReference<String> userInChain = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> userInChain.set(SecurityContext.getSimpleSecurityUser().getId()));

        assertThat(userInChain.get()).isEqualTo("u-1");
        assertThat(SecurityContext.getSimpleSecurityUser()).isNull();
    }

    @Test
    void invalidBearerTokenIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("AUTH001");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void wrongServerTokenIsUnauthorizedAndRightOneIsSystemAdmin() throws Exception {
        MockHttpServletRequest wrong = new MockHttpServletRequest("GET", "/orders");
        wrong.addHeader("Server-Token", "nope");
        MockHttpServletResponse wrongResponse = new MockHttpServletResponse();
        filter.doFilter(wrong, wrongResponse, new MockFilterChain());
        assertThat(wrongResponse.getStatus()).isEqualTo(401);

        MockHttpServletRequest right = new MockHttpServletRequest("GET", "/orders");
        right.addHeader("Server-Token", "server-key");
        AtomicReference<Boolean> admin = new AtomicReference<>();
        filter.doFilter(right, new MockHttpServletResponse(),
                (req, res) -> admin.set(SecurityContext.getSimpleSecurityUser().isSystemAdmin()));
        assertThat(admin.get()).isTrue();
    }

    @Test
    void publicPathsMatchByPrefixNotBySubstring() throws Exception {
        assertThat(properties.isPublicPath("/swagger-ui/index.html")).isTrue();
        assertThat(properties.isPublicPath("/swagger-ui.html")).isTrue();
        assertThat(properties.isPublicPath("/public/reports")).isTrue();
        // Contained but not a prefix: must still be authenticated
        assertThat(properties.isPublicPath("/admin/actuator/health")).isFalse();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    private static SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setServerKey("server-key");
        properties.getJwt().setSecretKey("0123456789abcdef0123456789abcdef");
        properties.getApiWhitelist().add("/public");
        return properties;
    }
}
