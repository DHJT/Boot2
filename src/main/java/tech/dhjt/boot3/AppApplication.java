package tech.dhjt.boot3;

import java.text.SimpleDateFormat;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {
        ElasticsearchDataAutoConfiguration.class,  // 禁用 Elasticsearch 数据自动配置
        ElasticsearchRestClientAutoConfiguration.class  // 禁用 Elasticsearch REST 客户端自动配置
})

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

}
