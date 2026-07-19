package tech.dhjt.boot.grpc.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot.grpc.server.proto.*;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/user1")
public class UserController1 {

    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;

    // ===================== 1. 创建用户 =====================

    @PostMapping("/create")
    public Map<String, Object> createUser() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("test_user")
                .setEmail("test@example.com")
                .setAge(25)
                .setPhone("13800138000")
                .setPassword("123456")
                .setNickname("测试用户")
                .build();

        CreateUserResponse resp = blockingStub.createUser(request);
        log.info("createUser -> userId={}", resp.getUserId());
        return Map.of("success", resp.getSuccess(), "message", resp.getMessage(), "userId", resp.getUserId());
    }

    // ===================== 2. 查询用户 =====================

    @GetMapping("/{userId}")
    public Map<String, Object> getUser(@PathVariable long userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();
        GetUserResponse resp = blockingStub.getUser(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", resp.getSuccess());
        result.put("message", resp.getMessage());
        if (resp.hasUser()) {
            result.put("user", toUserMap(resp.getUser()));
        }
        return result;
    }

    @PostMapping("/listUsers")
    public List<UserInfo> listUsers(@PathVariable long userId) {
        ListUsersRequest request = ListUsersRequest.newBuilder().setPageSize(3).build();
        Iterator<UserInfo> iterator = blockingStub.listUsers(request);

        List<UserInfo> users = new ArrayList<> ();
        iterator.forEachRemaining(users::add);
        return users;
    }

    // ===================== 工具方法 =====================

    private Map<String, Object> toUserMap(UserInfo user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("age", user.getAge());
        map.put("phone", user.getPhone());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }
}