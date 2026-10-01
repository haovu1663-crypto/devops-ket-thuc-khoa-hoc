# ==========================================
# Multi-stage Dockerfile for Product Service (Gradle + gRPC)
# PHẢI dùng JDK Debian (không phải Alpine) vì protoc-gen-grpc-java
# cần glibc để chạy trong quá trình build.
# ==========================================

# Stage 1: Build JAR (Debian-based JDK để protoc chạy được)
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image nhẹ hơn (JRE Alpine OK vì chỉ chạy JAR)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8082

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
