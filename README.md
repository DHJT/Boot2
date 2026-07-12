# README
Spring Boot 4

### 最低要求
- JDK 17
- Maven 3.6.3或更高的版本。
- Gradle 7.6.4或更高的版本。


### 新特性
1. 多版本支持
http://localhost:8080/test?version=1

2. 虚拟线程全局配置

通过spring.threads.virtual.enabled=true全局启用，原有@Async注解无需修改
```yml
spring:
  threads:
    virtual:
      enabled: true
```

3. GraalVM原生镜像深度集成

    - 冷启动效率：传统JVM模式下500ms启动的微服务，编译为原生镜像后降至50ms以内，适用于Serverless场景突发流量响应
    - 内存占用削减：典型微服务堆内存从2GB缩减至120MB级别，资源成本降低80%以上
    - 技术适配：通过@NativeHint注解显式配置反射与资源加载规则，官方提供Maven插件自动化分析依赖项兼容性

Spring Boot 4.0将GraalVM原生编译从实验特性升级为正式生产级支持，通过AOT（Ahead-of-Time）编译实现冷启动时间与内存占用的数量级优化：

http://localhost:8080/actuator/

```sh
curl http://localhost:8080/test  -H "version: 1"
```

http://localhost:8080/h2-console

http://localhost:8080/doc.html
http://localhost:8080/swagger-ui.html

```shell
mvn dependency:tree
mvn dependency:sources
# 下载特定依赖的源码
mvn dependency:sources -DincludeGroupIds=com.baomidou -DincludeArtifactIds=mybatis-plus-core
mvn clean package -DskipTests
# 无需项目，直接通过坐标下载
mvn dependency:get \
  -Dartifact=com.baomidou:mybatis-plus-core:3.5.9:jar:sources \
  -DremoteRepositories=https://repo1.maven.org/maven2/
```