package tech.dhjt.boot3.route;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import tech.dhjt.boot3.service.PersonService;

public class UserRoutes {
    public static RouterFunction<ServerResponse> OneUserRoutes(PersonService service) {
        return RouterFunctions.route().add(UserRoutes.GenUserRoutes()).add(UserRoutes.GenUserRoutes2(service)).build();
    }
    // publicstatic RouterFunction<ServerResponse> userRoutes(UserService
    // service) {
    public static RouterFunction<ServerResponse> GenUserRoutes() {
        return RouterFunctions.route().GET("/users", request -> ServerResponse.ok().bodyValue("ok"))
                .POST("/users", request -> ServerResponse.badRequest().bodyValue("TEST"))
                // 全局异常处理
                .onError(RuntimeException.class, (e, request) -> ServerResponse.status(500).bodyValue(e.getMessage()))
                .build();
    }
    public static RouterFunction<ServerResponse> GenUserRoutes2(PersonService service) {
        return RouterFunctions.route().GET("/admin/**", request -> {
            if (!checkAuth(request)) {
                return ServerResponse.status(401).build();
            }
            return handleAdminRequest(request);
        }).build();
    }
    public static RouterFunction<ServerResponse> GenUserRoutes3(PersonService service) {
        return RouterFunctions.route().GET("/api/{version}/data", request -> {
            // 根据请求头动态分发
            String version = request.headers().header("X-API-Version").get(0);
            return "v2".equals(version) ? handleV2(request) : handleV1(request);
        }).build();
    }
    private static Mono<ServerResponse> handleV1(ServerRequest request) {
        // TODO Auto-generated method stub
        return null;
    }
    private static Mono<ServerResponse> handleV2(ServerRequest request) {
        // TODO Auto-generated method stub
        return null;
    }
    private static Mono<ServerResponse> handleAdminRequest(ServerRequest request) {
        // TODO Auto-generated method stub
        return null;
    }
    private static boolean checkAuth(ServerRequest request) {
        // TODO Auto-generated method stub
        return false;
    }
}
