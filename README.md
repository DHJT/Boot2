# Boot4

基于 **Spring Boot 4.1.0（Java 22）** 的后端示例工程，集成 MyBatis-Plus、H2、Knife4j 等常用组件，
并演示了 Spring Boot 4 新特性（API 版本、虚拟线程、GraalVM 原生镜像）与 **Spring StateMachine 4.x** 状态机的完整用法。

---

## 一、技术栈

| 组件 | 说明 |
|---|---|
| Spring Boot | 4.1.0 |
| Java | 22 |
| MyBatis-Plus | 3.5.17（`mybatis-plus-spring-boot4-starter`） |
| 数据库 | H2 内存数据库（`jdbc:h2:mem:boot4db`） |
| Spring StateMachine | 4.0.2 |
| API 文档 | SpringDoc OpenAPI 3.0.3 + Knife4j 4.5.0 |
| 对象映射 | MapStruct 1.6.3 |
| 其他 | Lombok、Actuator、WebFlux（WebClient）、Validation |

### 环境要求
- JDK 22+
- Maven 3.6.3 或更高版本

---

## 二、快速开始

```shell
# 编译
mvn clean compile -DskipTests

# 打包
mvn clean package -DskipTests

# 运行
mvn spring-boot:run
# 或
java -jar target/boot4-0.0.1-SNAPSHOT.jar
```

默认端口 `8080`，激活 `dev` 环境（`application-dev.yml`）。

### 常用地址

| 用途 | 地址 |
|---|---|
| Knife4j 文档 | http://localhost:8080/doc.html |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 控制台 | http://localhost:8080/h2-console |
| Actuator | http://localhost:8080/actuator/ |

> H2 控制台连接信息：JDBC URL `jdbc:h2:mem:boot4db`，用户名 `sa`，密码为空。

---

## 三、项目结构

```
src/main/java/tech/dhjt/boot
├── AppApplication.java          # 启动类（@MapperScan）
├── bean/                        # 实体、DTO、VO
├── controller/                  # 业务接口（订单、用户、版本等）
├── convert/                     # MapStruct 转换器 & 枚举转换
├── config/                      # 通用配置（MyBatis-Plus、多租户、H2 Console、OpenApi 等）
├── enums/                       # 通用枚举体系（BaseEnum / IEnum）
├── handler/                     # 全局异常、统一响应 Result、TypeHandler
├── mapper/                      # MyBatis-Plus Mapper
├── service/                     # 业务服务
└── statemachine/                # ★ Spring StateMachine 演示模块
    ├── OrderStates.java         # 订单状态枚举
    ├── OrderEvents.java         # 订单事件枚举
    ├── config/                  # 各类状态机配置
    ├── persist/                 # 状态机数据库持久化
    ├── service/                 # 状态机演示服务
    └── controller/              # 状态机演示接口

src/main/resources
├── application.yml              # 激活 profile
├── application-dev.yml          # 开发环境配置
└── schema.sql                   # H2 建表脚本（订单表 + 状态机上下文表）
```

---

## 四、Spring Boot 4 新特性演示

### 1. API 版本（Versioning）
同一路径按版本号路由到不同实现：
```shell
curl "http://localhost:8080/test" -H "X-API-Version: 1.0"   # -> 1.0.0
curl "http://localhost:8080/test" -H "X-API-Version: 2.0"   # -> 2.0.0
curl "http://localhost:8080/test/3"                          # -> 3.0.0
```

### 2. 虚拟线程全局启用
通过配置全局开启，原有 `@Async` 无需修改：
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 3. GraalVM 原生镜像
Spring Boot 4 将 GraalVM 原生编译升级为正式生产级支持，通过 AOT 编译大幅优化冷启动时间与内存占用：
- 冷启动：500ms 级 → 50ms 内
- 内存占用：GB 级 → 百 MB 级

---

## 五、业务接口

### 订单接口（`/api/orders`）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/orders/listByStatus?orderstatus=PENDING` | 按状态查询 |
| POST | `/api/orders` | 创建订单（含枚举、JSON 字段、多租户填充） |
| PUT | `/api/orders/updateById` | 按 ID 更新（乐观锁版本） |
| DELETE | `/api/orders/{id}` | 逻辑删除 |
| GET | `/api/orders/selectPageByStatus/{pageNo}/{pageSize}/{status}` | 分页查询 |

---

## 六、状态机模块（Spring StateMachine 4.x）

依赖：
```xml
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-starter</artifactId>
    <version>4.0.2</version>
</dependency>
```

统一接口前缀：`/api/statemachine`。以下功能均可在 Knife4j 文档中直接调试。

### 1. 状态机持久化到数据库
订单状态机（`PENDING → PAID → DELIVERED → COMPLETED`，`PENDING → CANCELLED`）通过
自定义 `StateMachinePersist` + `JdbcTemplate`，将当前状态与扩展状态变量（JSON）落库到
`state_machine_context` 表；每次发送事件时先 `restore` 恢复、再 `persist` 保存。

```shell
# 支付（携带金额，写入扩展状态并持久化）
curl -X POST "http://localhost:8080/api/statemachine/order/ORD001/events/PAY?amount=200"
# 查询（从数据库恢复当前状态）
curl "http://localhost:8080/api/statemachine/order/ORD001"
# 后续事件
curl -X POST "http://localhost:8080/api/statemachine/order/ORD001/events/DELIVER"
curl -X POST "http://localhost:8080/api/statemachine/order/ORD001/events/RECEIVE"
```

### 2. Choice / Junction 伪状态
- **Choice**：按金额动态路由（`amount>=1000 → HIGH`，`>=100 → MEDIUM`，否则 `LOW`）
- **Junction**：按评分路由（`score>=90 → APPROVED`，`>=60 → MANUAL_REVIEW`，否则 `REJECTED`）

```shell
curl -X POST "http://localhost:8080/api/statemachine/choice?amount=500"
curl -X POST "http://localhost:8080/api/statemachine/junction?score=75"
```

### 3. Deferred Event（延迟事件）
`BUSY` 状态下收到的 `TASK` 事件被延迟入队（不丢弃），回到 `IDLE` 后自动重放。

```shell
curl -X POST "http://localhost:8080/api/statemachine/defer/demo"
```

### 4. 分层状态（Hierarchical / Substates）
父状态 `PROCESSING` 包含子状态 `VALIDATING`（初始）/ `PACKING`；
定义在父状态上的转移（`FINISH` / `ABORT`）在任意子状态下均可触发。

```shell
curl -X POST "http://localhost:8080/api/statemachine/hierarchical/demo"
```

### 5. 并发状态（Fork / Join，Orthogonal Regions）
`fork` 一次进入两个并行 Region（A：`A1→A2`，B：`B1→B2`），两个 Region 都到达末端后
`join` 汇合到 `DONE`。

```shell
curl -X POST "http://localhost:8080/api/statemachine/forkjoin/demo"
```

> 说明：`spring-statemachine 4.0.2` 官方基于 Spring Boot 3.x 构建，在本工程 Spring Boot 4.1 下经测试可用，
> 但未经官方认证；若升级依赖遇到兼容问题请留意版本匹配。

---

## 七、数据库

启动时通过 `schema.sql` 自动建表（`spring.sql.init.mode: always`）：
- `t_order`：订单表（含逻辑删除、乐观锁 version、多租户 tenant_id、JSON 字段）
- `state_machine_context`：状态机持久化上下文表（machine_id / state / extended_state）

---

## 八、常用 Maven 命令

```shell
mvn dependency:tree                 # 查看依赖树
mvn dependency:sources              # 下载依赖源码
mvn clean package -DskipTests       # 打包（跳过测试）

# 下载指定依赖源码
mvn dependency:sources -DincludeGroupIds=com.baomidou -DincludeArtifactIds=mybatis-plus-core

# 无需项目，直接通过坐标下载
mvn dependency:get \
  -Dartifact=com.baomidou:mybatis-plus-core:3.5.9:jar:sources \
  -DremoteRepositories=https://repo1.maven.org/maven2/
```
