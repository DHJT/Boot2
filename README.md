# Native Image
将项目编译为 Native Image的程序包


## 使用
- 使用 Maven Wrapper 进行项目构建
    - mvnw（Maven Wrapper）是 Maven 项目的轻量级封装工具，核心作用是 确保项目在不同环境中使用指定版本的 Maven 构建，避免因本地 Maven 版本不一致导致的构建问题。
    - 适用于团队协作或 CI/CD 场景，消除对全局 Maven 安装的依赖。
    - 首次运行 mvnw 时，若检测到环境中未安装配置的 Maven 版本，会自动从远程仓库下载对应版本并缓存到本地（默认路径 .mvn/wrapper）。

```bash
./mvnw clean install   # Linux/Mac
mvnw.cmd clean install # Windows
./mvnw native:compile -Pnative -DskipTests
```

## 安装
- GraalVM 24+36.1