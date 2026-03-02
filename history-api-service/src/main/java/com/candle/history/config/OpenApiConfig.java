package com.candle.history.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Candle Aggregation Service")
                        .version("1.0.0")
                        .description("API documentation using Candle Aggregation History api"));
    }

    @Bean
    public GroupedOpenApi historyApi() {
        return GroupedOpenApi.builder()
                .group("candle-history-apis")
                .packagesToScan("com.candle.history")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
