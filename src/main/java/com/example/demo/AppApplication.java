package com.example.demo;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AppApplication {

    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");//设置日期格式,精确到毫秒

	public static void main(String[] args) {
//	    System.exit(SpringApplication
//                .exit(SpringApplication.run(AppApplication.class, args)));
		SpringApplication.run(AppApplication.class, args);
	}

	/**
	 * @decription 定制程序退出码
	 * @author DHJT 2021-09-05 18:28:14.
	 * @return
	 */
	@Bean
	public ExitCodeGenerator exitCodeGenerator() {
	    Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("12314")));
	    return () -> 42;
	}

	@PostConstruct
	public void postConstruct() {
        System.out.println("时间：" + df.format(new Date()) + "执行@PostConstruct修饰的someMethod()方法...");
	}

	@PreDestroy
    public void preDestroy() {
        System.out.println("时间：" + df.format(new Date()) + "执行@PreDestroy修饰的otherMethod()方法...");
    }

}
