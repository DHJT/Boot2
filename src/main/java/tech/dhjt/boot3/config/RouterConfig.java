package tech.dhjt.boot3.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

//@EnableWebFlux
//@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> nestedRoutes() {
        return RouterFunctions.nest(path("/api"),
                RouterFunctions.route(GET("/users"), this::getUsers)
                .andRoute(POST("/users"), this::createUser)
                .andRoute(GET("/users/{id}"), this::getUserById));
    }

    @Bean
    public RouterFunction<ServerResponse> combinedRoutes() {
        RouterFunction<ServerResponse> userRoutes = RouterFunctions.route(GET("/users"), this::getUsers)
                .andRoute(POST("/users"), this::createUser)
                .andRoute(GET("/users/{id}"), this::getUserById);

        RouterFunction<ServerResponse> productRoutes = RouterFunctions.route(GET("/products"), this::getProducts)
                .andRoute(POST("/products"), this::createProduct)
                .andRoute(GET("/products/{id}"), this::getProductById);

        // 使用 and() 方法组合多个路由：在 Spring WebFlux 6.x 中，RouterFunctions.combine 已被移除，取而代之的是 RouterFunction.and() 方法。
        // 使用 and() 方法组合 4 个路由
        // userRoutes.and(productRoutes).and(orderRoutes).and(paymentRoutes);
        return userRoutes.and(productRoutes);
    }

    @Bean
    public HandlerFunction<ServerResponse> helloHandler() {
        return request -> ServerResponse.ok().body(Mono.just("Hello, World!"), String.class);
    }

    @Bean
    public RouterFunction<ServerResponse> helloRouter(HandlerFunction<ServerResponse> helloHandler) {
        return RouterFunctions.route(GET("/hello"), helloHandler);
    }

    @Bean
    public RouterFunction<ServerResponse> userRouter() {
        return RouterFunctions.route(GET("/user/{id}"), request -> {
            String userId = request.pathVariable("id");
            return ServerResponse.ok().body(Mono.just("User ID: " + userId), String.class);
        });
    }

    @Bean
    public RouterFunction<ServerResponse> deptRouter() {
        return RouterFunctions.route(GET("/dept/{id}"), request -> {
            String deptId = request.pathVariable("id");
            return ServerResponse.ok().body(Mono.just("Dept ID: " + deptId), String.class);
        });
    }

    private Mono<ServerResponse> getUsers(ServerRequest request) {
        // 处理获取用户列表的逻辑
        return ServerResponse.ok().bodyValue("List of users");
    }

    private Mono<ServerResponse> createUser(ServerRequest request) {
        // 处理创建用户的逻辑
        return ServerResponse.ok().bodyValue("User created");
    }

    private Mono<ServerResponse> getUserById(ServerRequest request) {
        // 处理获取单个用户的逻辑
        return ServerResponse.ok().bodyValue("User with id: " + request.pathVariable("id"));
    }

    private Mono<ServerResponse> getProducts(ServerRequest request) {
        // 处理获取产品列表的逻辑
        return ServerResponse.ok().bodyValue("List of products");
    }

    private Mono<ServerResponse> createProduct(ServerRequest request) {
        // 处理创建产品的逻辑
        return ServerResponse.ok().bodyValue("Product created");
    }

    private Mono<ServerResponse> getProductById(ServerRequest request) {
        // 处理获取单个产品的逻辑
        return ServerResponse.ok().bodyValue("Product with id: " + request.pathVariable("id"));
    }
}
