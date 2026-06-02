package com.wexinc.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WEX Transaction API")
                        .description("API for storing and retrieving purchase transactions with currency conversion support via US Treasury exchange rates.")
                        .version("1.0.0")
                        .license(new License()
                                .name("Proprietary")));
    }
}
