package tech.dhjt.boot3.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置 — JWT 无状态认证 + RBAC 权限控制
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 白名单路径 - 无需认证 */
    private static final String[] WHITELIST = {
            "/", "/index.html",
            "/static/**",
            "/h2-console/**",
            "/ws/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/favicon.ico",
            "/error",
            // 认证接口
            "/api/auth/**",
            // 静态页面资源
            "/workflow/**",
            // TODO 临时处理方案
            "/flowable/multi/start",
            "/flowable/multi/**",
            "/flowable/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF（JWT 无状态不需要）
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    AntPathRequestMatcher.antMatcher("/h2-console/**"),
                    AntPathRequestMatcher.antMatcher("/api/**")
                )
            )
            // 允许 H2 Console 使用 frame
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // 无状态会话（JWT）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 请求授权
            .authorizeHttpRequests(auth -> auth
                // 白名单完全开放
                .requestMatchers(WHITELIST).permitAll()
                .requestMatchers("/api/system/**").authenticated()
                .requestMatchers("/api/flowable/**").authenticated()
                .anyRequest().permitAll()
            ).csrf(csrf -> csrf.disable()) // 禁用 CSRF 保护
            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}