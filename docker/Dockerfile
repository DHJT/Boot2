ARG IMG_PRE
FROM ${IMG_PRE}openjdk:8-jdk-alpine
LABEL authors="dhjt"
ADD *.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
