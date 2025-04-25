package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {

    /**
     * @decription 根据打印顺序可以看到，首先是构造函数，也就是创建了bean，紧接着执行了init，然后再context.close要销毁bean之前又执行了destroy。
     * @author DHJT 2021-09-06 00:43:31.
     * @return
     */
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public CommonBean commonBean() {
        return new CommonBean();
    }

}
