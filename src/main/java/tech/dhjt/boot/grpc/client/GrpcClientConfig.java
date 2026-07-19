package tech.dhjt.boot.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.dhjt.boot.grpc.server.proto.UserServiceGrpc;

@Configuration
public class GrpcClientConfig {

    @Value("${grpc.client.host:localhost}")
    private String host;

    @Value("${grpc.client.port:9090}")
    private int port;

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel grpcChannel() {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public UserServiceGrpc.UserServiceBlockingStub blockingStub(ManagedChannel channel) {
        return UserServiceGrpc.newBlockingStub(channel);
    }
}