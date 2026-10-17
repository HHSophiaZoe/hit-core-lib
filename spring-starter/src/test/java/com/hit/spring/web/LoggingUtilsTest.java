package com.hit.spring.web;

import com.hit.spring.json.JsonMapper;
import com.hit.spring.web.filter.CachedBodyRequestWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingUtilsTest {

    private static final Set<String> SENSITIVE = Set.of("password", "otp");

    @BeforeAll
    static void initJsonMapper() {
        new JsonMapper(tools.jackson.databind.json.JsonMapper.builder().build());
    }

    @Test
    void masksSensitiveJsonFieldsAtAnyDepth() throws Exception {
        String body = LoggingUtils.getRequestBody(request("application/json",
                "{\"user\":\"a\",\"password\":\"p1\",\"items\":[{\"otp\":\"123\",\"qty\":2}]}"), SENSITIVE);

        assertThat(body).isEqualTo("{\"user\":\"a\",\"password\":\"***MASKED***\",\"items\":[{\"otp\":\"***MASKED***\",\"qty\":2}]}");
    }

    @Test
    void masksSensitiveFormFields() throws Exception {
        String body = LoggingUtils.getRequestBody(request("application/x-www-form-urlencoded", "user=a&password=p1"), SENSITIVE);

        assertThat(body).isEqualTo("[FORM-URLENCODED] user=a, password=***MASKED***");
    }

    @Test
    void keepsBodyWhenNothingIsSensitive() throws Exception {
        assertThat(LoggingUtils.getRequestBody(request("application/json", "{\"password\":\"p1\"}"), Set.of()))
                .isEqualTo("{\"password\":\"p1\"}");
    }

    private static CachedBodyRequestWrapper request(String contentType, String body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setContentType(contentType);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new CachedBodyRequestWrapper(request);
    }
}
