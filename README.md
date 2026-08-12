# Spring Boot 3 + Flowable 工作流平台

<!-- @author DHJT -->

基于 **Spring Boot 3.3.1 + Flowable 7.2.0 + Java 21 + H2** 的工作流审批平台，内置 **DMN 决策表** 驱动的请假审批流程（完整审批闭环）、多级复杂审批流程、JWT + RBAC 安全体系、实时通知（WebSocket）与前端管理台。

## 技术栈

| 组件 | 版本/说明 |
|------|-----------|
| Spring Boot | 3.3.1 |
| Flowable | 7.2.0（流程引擎 + DMN 决策引擎 + IDM） |
| Java | 21（`mvn` 需使用 JDK 21，JDK 24 会导致 Lombok 注解处理失效） |
| 数据库 | H2（开发：文件库 `./flowable_db`；测试：内存库） |
| 认证 | JWT（jjwt 0.12.6）+ Spring Security |
| 前端 | 原生 JS / Vue 3 + Element Plus 静态页（`src/main/resources/static/workflow/`） |
| 接口文档 | Knife4j（`http://localhost:8080/doc.html`） |

## 快速开始

```bash
# 1. 设置 JDK 21（本项目必须使用 JDK 21 构建）
set JAVA_HOME=D:\ProgramFiles\Java\jdk-21.0.11

# 2. 运行测试（20 个集成测试，覆盖 DMN 规则与请假/多级流程闭环）
mvn test

# 3. 启动应用
mvn spring-boot:run

# 4. 访问
#   - 前端管理台：http://localhost:8080/workflow/index.html（根路径 / 自动跳转）
#   - 请假独立演示页：http://localhost:8080/workflow/leave.html
#   - 多级审批演示页：http://localhost:8080/workflow/multiLevelApprovalProcess.html
#   - 接口文档：http://localhost:8080/doc.html
#   - H2 控制台：http://localhost:8080/h2-console（JDBC URL: jdbc:h2:file:./flowable_db，用户 sa）
```

> 注意：`static/workflow/` 目录名是 `workflow`（原 `wokflow` 为错拼，已重命名），与 `WebMvcConfig` 重定向及 `SecurityConfig` 白名单一致。

## 决策表（DMN）功能

应用启动时通过 `DmnAutoDeployRunner` 自动部署 3 张决策表（内容指纹幂等：内容未变化自动跳过，内容变化自动部署新版本；可通过 `app.dmn.auto-deploy` 关闭）。也可手动调用 `POST /dmn/deploy/all` 部署。

### 决策表清单

| 决策表 Key | 输入 | 输出 | 命中策略 | 用途 |
|------------|------|------|----------|------|
| `leaveApprovalPath` | days、deptName | approvalPath（advisor/dean）、finalNeedDeanApproval | FIRST | **流程路由单一决策来源**：请假流程网关据此分流辅导员/院长 |
| `leaveDaysDecision` | days | leaveCategory（short/long） | UNIQUE | 基础决策表：天数分类 |
| `leaveDepartmentDecision` | deptName | needDeanApproval | FIRST | 基础决策表：部门是否需要院长审批 |

### leaveApprovalPath 规则（规则顺序即优先级）

| # | 条件 | 审批路径 | 是否院长审批 |
|---|------|----------|--------------|
| 1 | days > 3 | dean | true |
| 2 | days ≤ 3 且 deptName = 行政部 | dean | true |
| 3 | days ≤ 3 且 deptName = 人事部 | dean | true |
| 4 | days ≤ 3 且 deptName = 技术部 | advisor | false |
| 5 | days ≤ 3 且 deptName = 财务部 | advisor | false |
| 6 | 兜底（其余部门） | advisor | false |

### 决策表 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dmn/decisions` | 已部署决策表列表（最新版本） |
| GET | `/dmn/decisions/{key}` | 决策表详情（输入/输出/规则数/命中策略） |
| GET | `/dmn/evaluate/days?days=` | 天数评估 |
| GET | `/dmn/evaluate/department?deptName=` | 部门评估 |
| GET | `/dmn/evaluate/combined?days=&deptName=` | 综合评估（基础表组合） |
| GET | `/dmn/evaluate/approval-path?days=&deptName=` | **审批路径评估（流程路由依据）** |
| POST | `/dmn/deploy/all` | 部署全部预定义决策表（内容指纹幂等） |
| POST | `/dmn/deploy?resourcePath=` | 部署单个决策表 |
| POST | `/dmn/deploy/upload` / `/dmn/deploy/content` | 上传/内容部署（见"已知限制"） |

前端管理台"决策表"页签（`index.html`）提供列表、规则详情与在线评估演示。

## 请假审批流程（含驳回重提的完整闭环）

`leave.bpmn20.xml`（key=`leaveProcess`）：

```
提交申请 → DMN决策评估(leaveApprovalPath) → 网关(天数/部门)
   ├─ finalNeedDeanApproval=false → 辅导员审批(zhangsan,lisi)
   └─ finalNeedDeanApproval=true  → 院长审批(dean组)
        └─ 审批结果网关：同意 → 结束
                       拒绝 → 退回提交人重新提交（可改天数/原因/部门）→ 重新 DMN 评估
```

- **同意**：`approved=true` 或 `approval='approved'` → 流程结束，通知申请人。
- **拒绝**：`approved=false` 或 `approval='rejected'` → 退回 `submitLeave`（assignee=申请人），前端提供"重新提交"编辑表单，重提后**重新执行 DMN 评估**（改天数/部门会改变审批路径）。
- **双模式兼容**：`POST /flowable/task/approve` 的 `approved` 参数同时接受 Boolean、字符串 `"true"/"false"`、字符串 `"approved"/"rejected"`（忽略大小写），服务端归一化后同时写入 `approved` 与 `approval` 两个流程变量，消除跨轮次/跨模式变量残留导致的误驳回。
- **驳回重提**：`POST /flowable/task/complete`（taskId + 表单变量 days/reason/deptName/applicantName/comment），仅放行白名单变量，流程保留变量（approved/approval/finalNeedDeanApproval 等）一律剥离。

## API 总览（实际映射路径）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录获取 JWT（JSON：username/password） |
| GET | `/auth/me` | 当前用户信息 |
| GET/POST/PUT/DELETE | `/system/users`、`/system/depts`、`/system/roles` | 用户/部门/角色 CRUD |
| POST | `/flowable/deploy?processKey=` | 部署流程（leaveProcess / multiLevelApprovalProcess） |
| POST | `/flowable/start` / `/flowable/start-with-dept` | 启动流程（后者支持 DMN 部门参数） |
| GET | `/flowable/all`、`/flowable/detail/{processInstanceId}` | 流程列表 + 详情时间线 |
| GET | `/flowable/definitions`、`/flowable/diagram` | 流程定义列表 / 流程图（支持高亮） |
| POST | `/flowable/task/approve?taskId=&approved=&comment=` | 审批（通过/驳回，双模式兼容） |
| POST | `/flowable/task/complete?taskId=&days=&reason=...` | **完成任务（驳回重提表单变量）** |
| POST | `/flowable/task/back`、`/back-to-node`、`/back-to-submitter` | 退回操作 |
| POST | `/flowable/task/claim|unclaim` | 任务认领/释放 |
| POST | `/flowable/process/suspend|activate|terminate` | 流程暂停/激活/终止 |
| POST | `/flowable/admin/task/transfer-temporary|transfer-permanent|add-approver|remove-approver|force-complete` | 管理操作（委派/转办/加签/去签/强过） |
| GET | `/flowable/notifications/{userId}`、`/flowable/tasks/*` | 通知与待办查询 |
| GET | `/dmn/**` | 见上节决策表 API |
| WS | `ws://localhost:8080/ws/notifications?userId=` | 实时通知推送 |

## 演示用户（密码均为 `123456`）

| 用户名 | 姓名 | 部门 | 角色 |
|--------|------|------|------|
| admin | 管理员 | 技术部 | ROLE_ADMIN, ROLE_USER |
| zhangsan | 张三 | 技术部 | ROLE_USER |
| lisi | 李四 | 技术部 | ROLE_USER, ROLE_MANAGER |
| wangwu | 王五 | 管理部 | ROLE_USER, ROLE_DIRECTOR |
| zhaoliu | 赵六 | 人力资源部 | ROLE_USER, ROLE_MANAGER |
| sunqi | 孙七 | 学生处 | ROLE_USER |
| zhouba | 周八 | 院办 | ROLE_USER, ROLE_DEAN |

> 辅导员审批候选用户：zhangsan、lisi；院长审批候选组：dean（zhouba）。

## 测试

`mvn test` 运行 20 个集成测试（内存 H2，`src/test/resources/application-test.yml`，不触碰开发库）：

| 测试类 | 覆盖 |
|--------|------|
| `DmnIntegrationTest`（5） | 三表部署幂等、天数/部门/审批路径全部规则路径、综合评估一致性 |
| `LeaveProcessIntegrationTest`（6） | 短假→辅导员、长假→院长、拒绝→驳回重提→重新DMN评估→同意结束、跨模式变量残留回归、变量双写入、时间线 |
| `ApproveNormalizationTest`（5） | 字符串 `true/false` 线格式审批、`/flowable/task/complete` 重提、保留变量剥离（防注入）、非提交类任务拒绝 |
| `MultiLevelProcessIntegrationTest`（4） | 短假通过（经理→hr 归档）、长假通过（经理→总监→归档）、驳回→重提→再审批闭环、Boolean 模式驱动 multi approval 网关回归 |

## 演示：改规则即改流程行为

决策表的威力：**改规则 → 重部署 → 流程行为立即变化**，无需改 BPMN 与 Java 代码。

1. 编辑 `src/main/resources/processes/leaveApprovalPath.dmn`，把规则 1 的条件从 `> 3` 改为 `> 5`（长假标准从 3 天放宽到 5 天）。
2. 重启应用（自动部署）或调用 `POST /dmn/deploy/all`（内容指纹检测到变化 → 自动部署新版本）。
3. 发起一笔 **4 天、技术部** 的请假：修改前走"院长审批"，修改后走"辅导员审批"——同一流程定义、同一代码，仅决策表变化即改变路由。

## 已知限制（安全项，已记录待办）

- 当前 `SecurityConfig` 为 `anyRequest().permitAll()` 全开放（JWT 过滤器存在但未强制拦截），`/flowable/**` 与 `/dmn/deploy/**` 均匿名可访问——**仅适合本地演示，不可直接暴露公网**。
- `GET /flowable/users` 等接口未脱敏；actuator 全量暴露（含 shutdown）；prod 配置含明文 `root/123456`。
- `/dmn/deploy/content`、`/dmn/deploy/upload` 匿名可覆盖同 key 决策表（业务路由篡改面），建议接入认证后使用。
- 以上事项见 `TO-DO.md` 安全清单，接入公网前必须处理。

## 项目结构

```
src/main/java/tech/dhjt/boot3/
├── controller/          # Auth / Flowable / System / Dmn / Home
├── service/
│   ├── dmn/             # DmnService（部署/评估/查询）、DmnEvaluationDelegate（流程内调用）、DmnAutoDeployRunner（自动部署）
│   ├── flowable/        # ProcessCommonService（审批/退回/待办/时间线/流程图）、ApprovalAdminService（管理操作）
│   └── impl/            # ProcessServiceImpl
├── listener/            # GlobalProcessEventListener（全局流程/任务事件 + 通知）
├── config/              # Security / JWT / WebSocket / FlowableGlobalConfigurer / WebMvcConfig
├── model/ repository/ enums/ event/
src/main/resources/
├── processes/           # leave.bpmn20.xml、multiLevelApprovalProcess.bpmn20.xml、3 张 DMN 决策表
└── static/workflow/     # index.html（管理台，含决策表页签）、leave.html、multiLevelApprovalProcess.html
src/test/java/tech/dhjt/boot3/   # DmnIntegrationTest、LeaveProcessIntegrationTest、ApproveNormalizationTest
```
