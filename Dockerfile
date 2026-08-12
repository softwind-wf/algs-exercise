# ============================================================
# University Website 镜像构建（多阶段）
# 阶段 1：Maven + JDK 8 构建可执行 jar（跳过测试，CI 已保证测试通过）
# 阶段 2：JRE 8 运行时镜像，体积小、攻击面小
#
# 构建：docker build -t university-web .
# 运行（连外部 MySQL）：
#   docker run -p 8080:8080 \
#     -e SPRING_DATASOURCE_URL='jdbc:mysql://host:3306/university?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
#     -e DB_USER=root -e DB_PASSWORD=root university-web
# 推荐直接用根目录 docker-compose.yml 一键起 app + MySQL + 监控栈。
# ============================================================

FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
# 先拷 pom 利用 Docker 层缓存预热依赖
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests \
    && find target -maxdepth 1 -name '*.jar' ! -name '*.original' -exec mv {} target/app.jar \;

FROM eclipse-temurin:8-jre
WORKDIR /app
# curl 供容器 HEALTHCHECK 探活 /actuator/health
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/app.jar app.jar
# 时区与编码：业务时间均为 Asia/Shanghai
ENV TZ=Asia/Shanghai JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
