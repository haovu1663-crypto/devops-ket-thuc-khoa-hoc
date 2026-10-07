# ==========================================
# Multi-stage Dockerfile for Payment Service
# ==========================================

# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
