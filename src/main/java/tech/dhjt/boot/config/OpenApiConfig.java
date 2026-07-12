package tech.dhjt.boot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Knife4j 文档自定义配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Boot4 系统 API")
                        .description("基于 Spring Boot 4 + 系统。")
                        .version("1.0.0")
                        .termsOfService("https://swagger.io/terms/")
                        .contact(new Contact()
                                .name("DHJT")
                                .email("admin@dhjt.tech")
                                .url("https://github.com/DHJT"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}