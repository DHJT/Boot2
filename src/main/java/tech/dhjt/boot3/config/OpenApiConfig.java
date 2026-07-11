package tech.dhjt.boot3.config;

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
                        .title("Boot3 工作流管理系统 API")
                        .description("基于 Spring Boot 3 + Flowable 的工作流审批系统，提供流程定义部署、请假审批、多级审批、系统管理（部门/角色/用户）等功能。")
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