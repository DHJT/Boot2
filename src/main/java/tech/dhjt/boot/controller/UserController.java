package tech.dhjt.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping(value = "/user", version = "1")
    public String getUserV1() {
        /* 旧版逻辑 */
        return "1";
    }

    @GetMapping(value = "/user", version = "2")
    public String getUserV2() {
        /* 新版逻辑 */
        return "1";
    }

    @GetMapping(value = "/users/{id}")
    public String getUserById(@PathVariable("id") String id) {
        return "3.0.0" + id;
    }
}
