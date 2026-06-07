package tech.dhjt.boot3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.HomeService;

import java.util.concurrent.*;

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

    @Operation(summary = "普通body请求")
    @PostMapping("/body")
    public ResponseEntity<User> body(@RequestBody User fileResp){
        return ResponseEntity.ok(fileResp);
    }

    @Operation(summary = "普通body请求+Param+Header+Path")
    @Parameters({
            @Parameter(name = "id",description = "文件id",in = ParameterIn.PATH),
            @Parameter(name = "token",description = "请求token",required = true,in = ParameterIn.HEADER),
            @Parameter(name = "name",description = "文件名称",required = true,in=ParameterIn.QUERY)
    })
    @PostMapping("/bodyParamHeaderPath/{id}")
    public ResponseEntity<User> bodyParamHeaderPath(@PathVariable String id, @RequestHeader("token") String token,
                                                    @RequestParam("name")String name, @RequestBody User fileResp) {
        fileResp.setName(fileResp.getName() + ",receiveName:" + name + ",token:" + token + ",pathID:" + id);
        return ResponseEntity.ok(fileResp);
    }

}
