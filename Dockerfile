# ============================================================
# 大学网站（Spring Boot 2.7 + JDK 8）多阶段构建
# 阶段一：Maven 编译打包；阶段二：精简 JRE 运行
# ============================================================
FROM maven:3.8-eclipse-temurin-8 AS builder
WORKDIR /build

# 先只拷贝 pom 预取依赖，利用 Docker 层缓存加速后续构建
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B dependency:go-offline -q || true

COPY src ./src
RUN mvn -B package -DskipTests -q

# ------------------------------------------------------------
FROM eclipse-temurin:8-jre
WORKDIR /app

# 以非 root 用户运行，降低容器逃逸风险
RUN useradd -r -u 1001 appuser

ARG JAR_FILE=target/algs4-1.0.0.0.jar
COPY --from=builder /build/${JAR_FILE} /app/app.jar

USER appuser
EXPOSE 8080

# 生产 profile：模板缓存开启；数据库连接由 compose 环境变量注入
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]