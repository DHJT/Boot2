# List of TO-DO

<!-- @author DHJT -->

--------------------------------

## 已交付（v1.1.0）

- [x] DMN 决策表完整落地：`leaveApprovalPath` 审批路径决策表（FIRST 规则）接入请假流程路由
- [x] DMN 启动自动部署（内容指纹幂等），`/dmn/deploy/all` 手动部署入口
- [x] 修复既有决策表缺陷：`leaveDepartmentDecision` 规则重叠、DMN 命名空间/非法 id/重复 id
- [x] 请假审批流程完整闭环：同意→结束；拒绝→退回提交人重新提交（重新 DMN 评估）
- [x] `approve` 入参归一化 + 双变量写入（修复前端字符串布尔误判导致的驳回静默丢失）
- [x] 新增 `POST /flowable/task/complete`（驳回重提，变量白名单 + 保留变量剥离）
- [x] 前端：目录重命名 `workflow`、决策表管理页签、驳回重提表单（三页面）
- [x] 集成测试 15 个用例全绿（DMN 规则 / 请假闭环 / 驳回重提 / 变量残留回归 / 归一化 / 注入剥离）
- [x] README / Changelog 同步

## 安全待办（接入公网前必须处理）

- [ ] SecurityConfig 全站 `permitAll` 放开——需改为真实认证（登录后放行），当前仅适合本地演示
- [ ] `/flowable/users` 等接口密码脱敏（当前返回明文密码）
- [ ] actuator 全量暴露（含 shutdown）——限制为 health 等白名单端点
- [ ] `application-prod.yml` 明文 `root/123456` 凭据——改为环境变量/密钥管理
- [ ] `/dmn/deploy/content`、`/dmn/deploy/upload` 匿名可覆盖同 key 决策表——需认证或 prod 禁用

## 后续建议

- [ ] CI 流水线（GitHub Actions：`mvn test` 门禁 + 自动构建镜像）
- [ ] 多级审批流程接入 DMN 路由（目前仅请假流程使用）
- [ ] 浏览器级 E2E 测试（Playwright/Selenium）
- [ ] 在线 DMN 设计器/编辑保存能力
- [ ] 删除 `diagram.dmn`（dmn-js 残留示例，无引用；本次已不参与部署）
