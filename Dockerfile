# Build stage: use official Gradle 9 image so "plugins { }" and modern build.gradle work.
# (Avoids "Unexpected input: '{'" when an older Gradle is used.)
FROM gradle:9.1.0-jdk17 AS builder

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN gradle bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
