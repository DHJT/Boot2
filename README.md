# Spring Boot 3.x
<!-- @author DHJT 2018-09-28 -->

<p align="center">
	<strong>A project of SpringBoot3.</strong>
</p>
<p align="center">
	<a href="http://search.maven.org/#artifactdetails%7Ccn.hutool%7Chutool-all%7C4.1.10%7Cjar">
		<img src="https://img.shields.io/badge/version-1.0-blue.svg" >
	</a>
	<a href="https://mit-license.org/">
		<img src="http://img.shields.io/:license-mit-blue.svg" >
	</a>
	<a>
		<img src="https://img.shields.io/badge/JDK-1.8+-green.svg" >
	</a>
</p>

#### 项目介绍
该项目是在Maven工具下建立的一个`Spring Boot 3`项目，可能生成各种版本和分支，用来改变项目的方向

#### 软件架构
- 工具说明:
    + 使用了Spring官方的STS工具
    + 或者使用IEDA进行编写
- 使用 Knife4j 进行接口文档展示（Knife4j 是在Swagger上进行了扩展）


#### 安装教程

1. xxxx
2. xxxx
3. xxxx

#### 使用说明

1. Knife4j 接口文档访问地址
http://localhost:8080/doc.html

2. Swagger 接口文档访问地址
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/swagger-ui/index.html

### 官方在 7.0.0 版本中已正式移除了传统的 Flowable UI 应用。
- 兼容性版本矩阵：UI模型器最高版本 6.8.1搭配 6.x 版核心且支持Spring Boot 2.x；7.0.0及之后不再提供且官方整体转向云/企业版设计工具支持Spring Boot 3.x。
- 无法降级使用的风险提示：理论上通过降级核心依赖强制使用旧版UI极不推荐，因为Flowable内部API在6.x和7.x之间发生了大量变化，硬性组合极易引发难以排查的类冲突或方法缺失等运行时错误。
- 定制开发与外部集成：业务系统已有一套前端架构需要嵌入流程设计能力时，可通过标准REST API与流程引擎交互或借助 bpmn-js 库自主构建。
- 企业版或官方设计工具：需要独立设计环境推荐使用免费的Flowable云端设计工具，生成标准BPMN XML可直接被引擎执行；要求私有化部署设计工具需联系官方获取Flowable Design等企业级产品与技术支持。

#### 参与贡献

## 改造完成总结

已成功完成项目的全面改造，以下是所有变更的详细清单：

### 一、💥 移除 Flowable 7.x 不兼容的 UI 组件（pom.xml）
- **已删除**：`flowable-spring-boot-starter-ui-modeler`、`flowable-spring-boot-starter-ui-task`、`flowable-spring-boot-starter-ui-admin`、`flowable-spring-boot-starter-ui-idm`（这些组件与 Flowable 7.2.0 不兼容）
- **保留**：`flowable-spring-boot-starter`（引擎核心）、`flowable-image-generator`（流程图生成）、`flowable-bpmn-layout`（自动布局）
- **新增**：`jjwt-api/impl/jackson` 0.12.6（JWT 令牌认证）

### 二、🔐 重构安全体系：JWT + RBAC
- [`SecurityConfig.java`](src/main/java/tech/dhjt/boot3/config/SecurityConfig.java:1)：Spring Security **JWT 无状态认证**，定义白名单路径（`/api/auth/**`、`/flowable/**`、`/ws/**` 等），添加 CORS 全局跨域配置
- [`JwtUtil.java`](src/main/java/tech/dhjt/boot3/config/JwtUtil.java:1)：JWT **令牌生成/解析/验证**工具，使用 HMAC-SHA256 签名，携带 userId/username/roles 等 claims
- [`JwtAuthenticationFilter.java`](src/main/java/tech/dhjt/boot3/config/JwtAuthenticationFilter.java:1)：**OncePerRequestFilter**，从 `Authorization: Bearer <token>` 提取并验证 JWT，构建 `UsernamePasswordAuthenticationToken` 存入 SecurityContext

### 三、📦 新建 RBAC 表结构（6张业务表）
| 表名 | 实体 | 功能 |
|------|------|------|
| `boot_user` | [`User.java`](src/main/java/tech/dhjt/boot3/model/po/User.java:1) | 用户（含部门、岗位、上级、组） |
| `boot_dept` | [`Dept.java`](src/main/java/tech/dhjt/boot3/model/po/Dept.java:1) | 部门（树形结构，支持层级路径） |
| `boot_role` | [`Role.java`](src/main/java/tech/dhjt/boot3/model/po/Role.java:1) | 角色（编码+描述） |
| `boot_user_role` | [`UserRole.java`](src/main/java/tech/dhjt/boot3/model/po/UserRole.java:1) | 用户-角色关联 |
| `boot_notification` | [`Notification.java`](src/main/java/tech/dhjt/boot3/model/po/Notification.java:1) | 通知消息（保留原表） |

### 四、🛠️ 服务层（Repository/Service/Controller）
- **Repository 层**：`DeptRepository`、`RoleRepository`、`UserRoleRepository`、`UserRepository`、`NotificationRepository`
- **Service 层**：
  - [`UserService.java`](src/main/java/tech/dhjt/boot3/service/UserService.java:1)：JWT 登录（返回 token）、用户 CRUD、7 个演示用户 @PostConstruct 初始化
  - [`DeptService.java`](src/main/java/tech/dhjt/boot3/service/DeptService.java:1)：部门树形管理、treePath 计算、CRUD
  - [`RoleService.java`](src/main/java/tech/dhjt/boot3/service/RoleService.java:1)：角色管理、用户角色分配/查询
  - [`NotificationService.java`](src/main/java/tech/dhjt/boot3/service/NotificationService.java:1)：通知持久化+WebSocket 推送（保留原逻辑）
- **Controller 层**：
  - [`AuthController.java`](src/main/java/tech/dhjt/boot3/controller/AuthController.java:1)：`POST /api/auth/login`（JSON 传参）
  - [`SystemController.java`](src/main/java/tech/dhjt/boot3/controller/SystemController.java:1)：`/api/system/depts`、`/roles`、`/users` 完整 CRUD + 用户角色分配 + @PostConstruct 演示数据初始化（5 部门 + 5 角色 + 7 用户角色分配）
  - [`FlowableController.java`](src/main/java/tech/dhjt/boot3/controller/FlowableController.java:1)：路由前缀改为 `/api/flowable`，保留全部工作流接口

### 五、🔄 流程审批与通知完善
- [`LeaveTaskListener.java`](src/main/java/tech/dhjt/boot3/listener/LeaveTaskListener.java:1)：任务**创建时**通知指定审批人和候选组成员；任务**完成时**通知申请人审批结果
- [`NotificationEvent.java`](src/main/java/tech/dhjt/boot3/event/NotificationEvent.java:1)：Spring Event 发布/订阅模式
- [`WebSocketConfig.java`](src/main/java/tech/dhjt/boot3/config/WebSocketConfig.java:1)：WebSocket `/ws/notifications`，按 userId 推送实时通知

### 六、🎨 前端 SPA 管理页面
[`index.html`](src/main/resources/static/wokflow/index.html:1) — 完整的单页应用（470 行），包含：
- **登录页**：演示账号提示（admin/zhangsan/lisi/wangwu），JWT Token 存储到 localStorage
- **控制台**：统计数据（流程数、待办数、未读通知数、用户数）、最近流程/待办列表
- **流程管理**：部署流程、发起请假、流程列表、流程图查看、审批时间线
- **我的待办**：待办任务列表，支持**通过/驳回**审批操作
- **用户管理**：用户 CRUD、用户角色分配
- **部门管理**：部门 CRUD
- **角色管理**：角色 CRUD
- **通知中心**：通知列表、标记已读/全部已读
- **WebSocket 实时推送**：浏览器自动连接 WebSocket，接收实时通知

### 七、🏗️ API 接口总览

| 路径 | 功能 | 认证 |
|------|------|------|
| `POST /api/auth/login` | 登录获取 JWT | 无需 |
| `GET /api/auth/me` | 获取当前用户 | 需要 Token |
| `GET/POST/PUT/DELETE /api/system/depts` | 部门 CRUD | 需要 Token |
| `GET/POST/PUT/DELETE /api/system/roles` | 角色 CRUD | 需要 Token |
| `GET/POST/PUT/DELETE /api/system/users` | 用户 CRUD | 需要 Token |
| `GET/POST /api/system/users/{id}/roles` | 用户角色分配 | 需要 Token |
| `POST /api/flowable/deploy` | 部署请假流程 | 需要 Token |
| `POST /api/flowable/start` | 发起请假 | 需要 Token |
| `GET /api/flowable/all` | 所有流程列表 | 需要 Token |
| `GET /api/flowable/detail/{id}` | 流程详情+时间线 | 需要 Token |
| `POST /api/flowable/multi/complete` | 审批（通过/驳回） | 需要 Token |
| `GET /api/flowable/notifications/{userId}` | 查询通知 | 需要 Token |
| `POST /api/flowable/notifications/read/{id}` | 标记已读 | 需要 Token |
| `ws://localhost:8080/ws/notifications?userId=` | WebSocket 实时推送 | 无 |

### 八、演示用户及角色
| 用户名 | 姓名 | 部门 | 角色 |
|--------|------|------|------|
| admin | 管理员 | 技术部 | ROLE_ADMIN, ROLE_USER |
| zhangsan | 张三 | 技术部 | ROLE_USER |
| lisi | 李四 | 技术部 | ROLE_USER, ROLE_MANAGER |
| wangwu | 王五 | 管理部 | ROLE_USER, ROLE_DIRECTOR |
| zhaoliu | 赵六 | 人力资源部 | ROLE_USER, ROLE_MANAGER |
| sunqi | 孙七 | 学生处 | ROLE_USER |
| zhouba | 周八 | 院办 | ROLE_USER, ROLE_DEAN |

> **所有用户密码均为 `123456`**

访问 `http://localhost:8080/flowable/index.html` 即可使用完整的流程审批管理系统。