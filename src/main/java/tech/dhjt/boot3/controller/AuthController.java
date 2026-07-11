package tech.dhjt.boot3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot3.service.UserService;

import java.util.Map;

/**
 * 认证 REST API — 登录/注册
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户登录认证相关接口")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        return userService.login(username, password);
    }

    @Operation(summary = "获取当前用户信息", description = "从JWT中解析当前登录用户信息（需在请求头添加 Authorization: Bearer <token>）")
    @GetMapping("/me")
    public Map<String, String> me() {
        return Map.of(
            "message", "请在请求头添加 Authorization: Bearer <token> 访问受保护接口",
            "note", "实际用户信息从 JWT Token 解析"
        );
    }
}