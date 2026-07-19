package tech.dhjt.boot.grpc.server;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import tech.dhjt.boot.grpc.server.proto.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 内存存储模拟数据库
    private final ConcurrentHashMap<Long, UserInfo> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // ===================== 1. 创建用户 =====================

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        long userId = idGenerator.getAndIncrement();
        String now = LocalDateTime.now().format(DTF);

        UserInfo user = UserInfo.newBuilder()
                .setId(userId)
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setAge(request.getAge())
                .setPhone(request.getPhone())
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .build();

        userStore.put(userId, user);

        CreateUserResponse response = CreateUserResponse.newBuilder()
                .setSuccess(true)
                .setMessage("用户创建成功")
                .setUserId(userId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        log.info("===== createUser 响应: userId={} =====", userId);
    }

    // ===================== 2. 查询用户 =====================

    @Override
    public void getUser(GetUserRequest request,
                        StreamObserver<GetUserResponse> responseObserver) {
        log.info("===== getUser: userId={} =====", request.getUserId());
        long userId = request.getUserId();
        UserInfo user = userStore.get(request.getUserId());
        user = Optional.of(user).orElseThrow(() -> Status.NOT_FOUND
                .withDescription("User not found: " + userId)
                .asRuntimeException(new Metadata()));

        GetUserResponse response = GetUserResponse.newBuilder()
                .setSuccess(true)
                .setMessage("查询成功")
                .setUser(user)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<UserInfo> responseObserver) {
        List<UserInfo> users = userStore.values().stream()
                .limit(request.getPageSize())
                .toList();

        for (UserInfo user : users) {
            UserInfo response = UserInfo.newBuilder()
                    .setId(user.getId())
                    .setUsername(user.getUsername())
                    .setEmail(user.getEmail())
                    .setAge(user.getAge())
                    .build();
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }
}
