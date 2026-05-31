package tech.dhjt.boot3.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot3.service.UserService;

import java.util.Map;

/**
 * 认证 REST API — 登录/注册
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        return userService.login(username, password);
    }

    /**
     * 获取当前用户信息（从JWT中解析，这里返回测试信息）
     * 实际应用中可以通过 SecurityContextHolder 获取
     */
    @GetMapping("/me")
    public Map<String, String> me() {
        return Map.of(
            "message", "请在请求头添加 Authorization: Bearer <token> 访问受保护接口",
            "note", "实际用户信息从 JWT Token 解析"
        );
    }
}