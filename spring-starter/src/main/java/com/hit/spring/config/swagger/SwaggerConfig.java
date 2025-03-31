package com.hit.spring.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String API_KEY = "Bearer Token";

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-apis")
                .pathsToMatch("/**")
                .packagesToScan("com.hit")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI().info(
                new Info()
                        .title("Project Base Spring API")
                        .version("1.0")
                        .description("Documentation Project Base Spring API v1.0"));

        openAPI.components(new Components().addSecuritySchemes(API_KEY,
                new SecurityScheme()
                        .name("Authorization")
                        .scheme("Bearer")
                        .bearerFormat("JWT")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER))
        );
        openAPI.addSecurityItem(new SecurityRequirement().addList(API_KEY));
        return openAPI;
    }

}