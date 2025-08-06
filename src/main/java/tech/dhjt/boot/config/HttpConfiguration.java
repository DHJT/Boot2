package tech.dhjt.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tech.dhjt.boot.service.TodoService;

//proxyBeanMethods = false:多实例对象，无论被取出多少此都是不同的bean实例，在该模式下SpringBoot每次启动会跳过检查容器中是否存在该组件
@Configuration(proxyBeanMethods = false)
public class HttpConfiguration {

    // 自动代理注入
    //    @Bean
    //    HttpServiceProxyFactory proxyFactory(RestClient.Builder builder) {
    //        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build();
    //    }

    //创建服务接口的代理对象，基于WebClient
    @Bean
    TodoService requestService() {
        WebClient webClient = WebClient.builder().baseUrl("http://jsonplaceholder.typicode.com").build();

        //创建代理工厂,设置超时时间
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();

        //创建某个接口的代理服务
        return proxyFactory.createClient(TodoService.class);
    }

    //定制HTTP服务
    //    @Bean
    TodoService albumsService() {
        //超时
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)//连接时间
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(10));//读超时
                    conn.addHandlerLast(new WriteTimeoutHandler(10));//写超时
                });
        //设置异常
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                //定制 4XX,5XX 的回调函数
                .defaultStatusHandler(HttpStatusCode::isError, clientResponse -> {
                    System.out.println("WebClient请求异常");
                    return Mono.error(new RuntimeException("请求异常" + clientResponse.statusCode().value()));
                }).build();

        //        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).blockTimeout(Duration.ofSeconds(60)).build();
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return proxyFactory.createClient(TodoService.class);
    }


}

