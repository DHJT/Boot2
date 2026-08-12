# Changelog

<!-- @author DHJT -->

------------------------------------------------------------------------------------------------------------

#### 1.1.0（当前）

###### 新特性：决策表完整落地 + 请假审批闭环

- **DMN 决策表完整落地**
  - 新增 `leaveApprovalPath.dmn`：按「请假天数 + 部门」综合判定审批路径（advisor/dean），作为请假流程路由的单一决策来源（hitPolicy=FIRST，规则有序防重叠）。
  - 新增 `DmnAutoDeployRunner`：应用启动自动部署全部决策表（内容指纹幂等——内容未变化跳过、变化自动部署新版本），可用 `app.dmn.auto-deploy` 关闭。
  - 修复 `leaveDepartmentDecision.dmn` 既有缺陷：默认规则与 UNIQUE 重叠、DMN 1.1 命名空间错误（https→http）、`informationRequirement` 非法 id、重复 id。
  - 规范化 `leaveDaysDecision.dmn`：消除 Camunda/dmn-js 命名空间兼容风险（改为 Flowable 原生 DMN 1.1 格式）。
  - `DmnService` 新增 `evaluateApprovalPath`；`DmnEvaluationDelegate` 改调审批路径表并写回 `approvalPath`/`finalNeedDeanApproval`（取变量/强转防御化）。
  - 新增 `GET /dmn/evaluate/approval-path`；前端新增"决策表"页签（列表/规则详情/在线评估）。
- **请假审批流程完整闭环（含驳回重提）**
  - `leave.bpmn20.xml` 新增审批结果网关：同意→结束；拒绝→退回提交人重新提交（重新 DMN 评估）。
  - `ProcessCommonService.approve` 入参归一化（Boolean / 字符串 `true|false` / `approved|rejected`，忽略大小写）+ 双变量写入（approved + approval），修复前端字符串布尔被误判导致"驳回静默丢失"的既有 bug，消除跨轮次/跨模式变量残留。
  - 新增 `POST /flowable/task/complete`：携带表单变量完成任务（驳回重提），仅放行白名单变量、剥离流程保留变量（防注入）、days 类型归一化。
  - 前端重命名 `static/wokflow/` → `static/workflow/`（与重定向/白名单对齐）；`multiLevelApprovalProcess.html` 修正不存在的 diagram 端点；`index.html`/`leave.html`/`multiLevelApprovalProcess.html` 增加"重新提交"表单。
- **测试与文档**
  - 新增 4 个集成测试类共 20 个用例（DMN 规则全路径、请假流程闭环、驳回重提、变量残留回归、字符串布尔线格式、保留变量剥离、非提交类任务防护、多级流程回归），`mvn test` 全绿（内存 H2，与开发库隔离）。
  - README 重写（API 路径修正、决策表说明、演示步骤、已知限制）；Changelog 同步；TO-DO 更新。

###### 修复

- `NotificationService.handleNotificationEvent`：`Map.of` 遇 null 值（流程结束通知的 taskId 为空）抛 NPE 并导致审批事务回滚——改为 null 安全的 HashMap 构建。
- 测试环境 JDK 兼容：Maven 需使用 JDK 21（JDK 24 下 Lombok 1.18.32 注解处理失效，属构建环境问题，非代码缺陷）。

------------------------------------------------------------------------------------------------------------

#### 1.0.0

###### 新特性

- 项目重构为 Spring Boot 3 + Flowable 7.2.0。
- 移除 Flowable 7.x 不兼容的 UI 组件，引入 JWT + RBAC 安全体系（jjwt 0.12.6）。
- 新建 5 张业务表（用户/部门/角色/用户角色/通知）+ 演示数据初始化。
- 请假审批流程（leaveProcess）、多级复杂审批流程（multiLevelApprovalProcess）示例。
- DMN 决策表基础支持（leaveDaysDecision、leaveDepartmentDecision）与部署/评估/查询 API。
- 全局流程事件监听 + WebSocket 实时通知。
- 前端 SPA 管理台（登录/控制台/流程管理/待办/用户/部门/角色/通知中心）。
