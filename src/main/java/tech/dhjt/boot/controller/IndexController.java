package tech.dhjt.boot.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }

    // 异步任务无需修改
    @Async
    public CompletableFuture<String> fetchData() {
        return CompletableFuture.completedFuture("Data from virtual thread");
    }

}
