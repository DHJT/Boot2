//package tech.dhjt.boot3.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
//import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
//import org.springframework.boot.web.reactive.error.ErrorAttributes;
//import org.springframework.context.ApplicationContext;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.web.reactive.function.server.RouterFunction;
//import org.springframework.web.reactive.function.server.RouterFunctions;
//import org.springframework.web.reactive.function.server.ServerRequest;
//import org.springframework.web.reactive.function.server.ServerResponse;
//import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;
//
//import reactor.core.publisher.Mono;
//
////@Component
//public class MyErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
//
//    public MyErrorWebExceptionHandler(@Autowired ErrorAttributes errorAttributes, Resources resources,
//            ApplicationContext applicationContext) {
//        super(errorAttributes, resources, applicationContext);
//    }
//
//    @Override
//    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
//        return RouterFunctions.route(this::acceptsXml, this::handleErrorAsXml);
//    }
//
//    private boolean acceptsXml(ServerRequest request) {
//        return request.headers().accept().contains(MediaType.APPLICATION_XML);
//    }
//
//    public Mono<ServerResponse> handleErrorAsXml(ServerRequest request) {
//        BodyBuilder builder = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
//        // ... additional builder calls
//        return builder.build();
//    }
//
//}
