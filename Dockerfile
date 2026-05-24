FROM openjdk:11-jre-slim
LABEL authors="DHJT"

ENTRYPOINT ["top", "-b"]
# 生成图片所需的 fontconfig 和字体库
RUN apt-get update && apt-get install -y fontconfig ttf-dejavu && rm -rf /var/lib/apt/lists/*
# 在 Dockerfile 中安装中文字体
RUN apt-get update && apt-get install -y fonts-noto-cjk
