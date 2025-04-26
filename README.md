# Boot 2
<!-- @author DHJT 2018-09-28 -->

<p align="center">
	<strong>A demo of SpringBoot2.</strong>
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
该项目是在Maven工具下建立的一个SpringBoot2项目，可能生成各种版本和分支，用来改变项目的方向

#### 软件架构


#### 安装教程

#### 使用说明

1. [网站首页](http://127.0.0.1:8080/springboot2)

http://localhost:8080/doc.html

```shell
mysql -uroot -p
```
```sql
 CREATE DATABASE boot;
```

### 部署

#### Docker
1. 在 Docker 容器中遇到`library initialization failed - unable to allocate file descriptor table - out of memory`错误时，通常是由于 文件描述符（File Descriptor, FD）数量不足 或 内存资源不足 导致的。

步骤 1：调整宿主机的文件描述符限制

```bash
# 查看当前 FD 限制
ulimit -n

# 临时设置 FD 限制（立即生效，重启失效）
ulimit -n 65535

# 永久设置（需修改系统配置文件）
# 编辑 /etc/security/limits.conf，添加：
* soft nofile 65535
* hard nofile 65535
```

步骤 2：调整 Docker 容器的 FD 限制

运行容器时通过 `--ulimit` 参数覆盖默认限制：

```bash
docker run -d --ulimit nofile=65535:65535 --name my_container my_image
```
步骤 3：验证容器内的 FD 限制

进入容器并检查：

```bash
docker exec -it my_container /bin/bash
ulimit -n  # 应输出 65535
```

2. 增加容器的内存限制

运行容器时通过 `-m` 指定内存上限（如 4GB）：

```bash
docker run -d -m 4g --name my_container my_image
```

#### 参与贡献
