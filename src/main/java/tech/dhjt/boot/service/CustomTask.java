package tech.dhjt.boot.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CustomTask {

    // 异步任务无需修改
    @Async
    public CompletableFuture<String> fetchData() {
        return CompletableFuture.completedFuture("Data from virtual thread");
    }

}
