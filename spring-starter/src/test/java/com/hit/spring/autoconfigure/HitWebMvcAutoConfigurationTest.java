package com.hit.spring.autoconfigure;

import com.hit.common.model.ResponseStatus;
import com.hit.common.model.pagination.PageableReqModel;
import com.hit.spring.web.bind.PaginationParameter;
import com.hit.spring.exception.BaseResponseException;
import com.hit.spring.web.response.GeneralResponse;
import com.hit.spring.web.response.ResponseFactory;
import com.hit.spring.security.filter.AuthenticationFilter;
import com.hit.spring.task.ExecutorManager;
import com.hit.spring.http.HttpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The starter wired only through auto-configuration (no component scan of com.hit), called over MockMvc.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = HitWebMvcAutoConfigurationTest.TestApplication.class, properties = {
        "app.task.executor.enable=true",
        "http-client.default.enable=true"
})
class HitWebMvcAutoConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void registersStarterBeansFromAutoConfiguration() {
        assertThat(context.getBeansOfType(ExecutorManager.class)).hasSize(1);
        assertThat(context.getBeansOfType(HttpService.class)).hasSize(1);
        assertThat(context.containsBean("appTaskExecutor")).isTrue();
        assertThat(context.containsBean("authenticationFilter")).isFalse();
        assertThat(context.getBeansOfType(AuthenticationFilter.class)).isEmpty();
    }

    @Test
    void successUsesStarterMessagesAndVietnameseByDefault() throws Exception {
        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(header().exists("trace_id"))
                .andExpect(jsonPath("$.status.code").value("SUCCESS"))
                .andExpect(jsonPath("$.status.message").value("Thành công"))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void acceptLanguageSelectsTheBundle() throws Exception {
        mockMvc.perform(get("/test/ok").header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8"))
                .andExpect(jsonPath("$.status.message").value("Success"));
    }

    @Test
    void callerTraceIdIsEchoed() throws Exception {
        mockMvc.perform(get("/test/ok").header("trace_id", "abc123"))
                .andExpect(header().string("trace_id", "abc123"));
    }

    @Test
    void malformedPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/test/items/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status.message.id").value("must be a valid UUID"));
    }

    @Test
    void malformedJsonBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidBodyListsTheFields() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status.message.name").value("must not be blank"));
    }

    @Test
    void responseStatusExceptionIsReturnedAsIs() throws Exception {
        mockMvc.perform(get("/test/custom-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status.code").value("CUSTOM"))
                .andExpect(jsonPath("$.status.message").value("custom message"));
    }

    @Test
    void unexpectedErrorIsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status.code").value("GENERAL_SERVER_ERROR"));
    }

    @Test
    void paginationParameterIsParsedByTheStarterResolver() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "2").param("page_size", "5").param("sorts", "name,asc"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.sort").value("name:ASC"));
    }

    record ItemRequest(@NotBlank String name) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/ok")
        ResponseEntity<GeneralResponse<String>> ok() {
            return ResponseFactory.success("ok");
        }

        @GetMapping("/test/items/{id}")
        ResponseEntity<GeneralResponse<UUID>> item(@PathVariable UUID id) {
            return ResponseFactory.success(id);
        }

        @PostMapping("/test/items")
        ResponseEntity<GeneralResponse<String>> create(@Valid @RequestBody ItemRequest request) {
            return ResponseFactory.success(request.name());
        }

        @GetMapping("/test/custom-status")
        void customStatus() {
            throw new BaseResponseException(new ResponseStatus("CUSTOM", "custom message"));
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/test/page")
        Map<String, Object> page(@PaginationParameter PageableReqModel request) {
            return Map.of("page", request.getPage(), "pageSize", request.getPageSize(),
                    "sort", request.getSorts().getFirst().getName() + ":" + request.getSorts().getFirst().getDirection());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @org.springframework.context.annotation.Bean
        TestController testController() {
            return new TestController();
        }
    }
}
