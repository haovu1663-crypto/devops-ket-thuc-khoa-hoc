# ==========================================
# Multi-stage Dockerfile for Order Service (Gradle + gRPC)
# PHẢI dùng JDK Debian (không phải Alpine) vì protoc-gen-grpc-java
# cần glibc để chạy trong quá trình build.
# ==========================================

# Stage 1: Build JAR (Debian-based để protoc chạy được)
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -q

# Stage 2: Runtime (JRE Alpine OK vì chỉ chạy JAR)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
