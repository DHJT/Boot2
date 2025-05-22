package tech.dhjt.boot3.config;

import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHandlingConfig {

    // @Bean
    ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes();
    }
}
