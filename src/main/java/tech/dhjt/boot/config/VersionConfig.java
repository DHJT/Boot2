package tech.dhjt.boot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class VersionConfig implements WebMvcConfigurer {

    /**
     * API版本控制
     */
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestParam("version");
        configurer.useRequestHeader("version");
        configurer.useRequestHeader("X-Version");

        //Add resolver to extract the version from a path segment.
        //Params:
        //index – the index of the path segment to check;
        // e.g. for URL's like "/{version}/..." use index 0, for "/api/{version}/..." index 1.
        //        configurer.usePathSegment(1);
        configurer.setDefaultVersion("1");
    }
}
