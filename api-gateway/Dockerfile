# ==========================================
# Multi-stage Dockerfile for API Gateway (Gradle)
# ==========================================

# Stage 1: Build JAR application using Gradle Wrapper
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Gradle wrapper và cấu hình dependencies để tận dụng Docker layer caching
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Cấp quyền thực thi cho gradlew
RUN chmod +x gradlew

# Copy toàn bộ mã nguồn và build jar
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image tối ưu và bảo mật
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy file jar từ builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Phân quyền cho non-root user
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
