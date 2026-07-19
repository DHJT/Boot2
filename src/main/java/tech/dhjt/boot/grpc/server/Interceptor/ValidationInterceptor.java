package tech.dhjt.boot.grpc.server.Interceptor;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import io.grpc.*;
import org.springframework.grpc.server.GlobalServerInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GlobalServerInterceptor
public class ValidationInterceptor implements ServerInterceptor {

    private final Validator validator;

    public ValidationInterceptor() {
        this.validator = ValidatorFactory.newBuilder().build();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            @Override
            public void onMessage(ReqT message) {
                // 仅验证 protobuf 消息
                if (message instanceof Message protoMsg) {
                    try {
                        ValidationResult result = validator.validate(protoMsg);
                        if (!result.isSuccess()) {
                            log.warn("Validation failed: {}", result.getViolations());
                            call.close(Status.INVALID_ARGUMENT
                                    .withDescription("Validation failed: " + result.getViolations()), headers);
                            return;
                        }
                    } catch (ValidationException e) {
                        log.error("Validation error", e);
                        call.close(Status.INTERNAL
                                .withDescription("Internal validation error: " + e.getMessage()), headers);
                        return;
                    }
                }
                super.onMessage(message);
            }
        };
    }
}
