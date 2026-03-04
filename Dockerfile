# Stage 1: Build
FROM gradle:8.14.4-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN gradle bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]