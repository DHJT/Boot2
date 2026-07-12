package tech.dhjt.boot.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {

//    @Bean
//    public ServletRegistrationBean<JakartaWebServlet> h2Console() {
//        ServletRegistrationBean<JakartaWebServlet> reg = new ServletRegistrationBean<>(new JakartaWebServlet());
//        reg.addUrlMappings("/h2-console/*");
//        // 允许远程访问（生产务必关闭）
//        reg.addInitParameter("webAllowOthers", "true");
//        return reg;
//    }
}
