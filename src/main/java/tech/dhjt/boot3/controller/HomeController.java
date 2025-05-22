package tech.dhjt.boot3.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.dhjt.boot3.service.HomeService;

@RestController
@RequestMapping("/")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private HomeService homeService;

    @GetMapping("/home")
    public String getResponse() {
        return homeService.getResponse();
    }

    @GetMapping("/home1")
    public Future<String> getResponse1() {
        return homeService.getResponse1();
    }

    @GetMapping("/home2")
    public Future<String> getResponse2() {
        long start = System.currentTimeMillis();
        CompletableFuture<String> future = homeService.getResponse1();
        // 计算结果完成时的回调方法
        try {
            future.whenComplete((k, v) -> {
                System.out.println("返回k=" + k);
                System.out.println("异常v=" + v);
            }).exceptionally(e -> {
                System.out.println("捕获异常=" + e.getMessage());
                return "okk";
            }).get(4000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        // 耗时时间
        float totalTime = (float)(System.currentTimeMillis() - start) / 1000;
        logger.info("total time: " + totalTime + " seconds");
        return future;
    }

}
