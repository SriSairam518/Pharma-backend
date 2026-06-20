# ================================================================
# Dockerfile  —  Multi-stage build for Spring Boot
#
# WHY MULTI-STAGE?
# Stage 1 (build): uses a full Maven + JDK image to compile your code
#                   into a runnable JAR. This image is large (~600MB)
#                   but we DON'T ship it — we only keep the JAR it produces.
# Stage 2 (run):    uses a small JRE-only image and copies just the
#                   compiled JAR into it. Final image is much smaller
#                   (~200MB instead of ~600MB) — faster deploys on Render.
#
# This is the standard production pattern for Java apps.
# ================================================================

# ---- STAGE 1: BUILD ----
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml FIRST (before source code).
# WHY THIS ORDER MATTERS:
# Docker caches each instruction as a "layer". If pom.xml hasn't
# changed, Docker reuses the cached dependency download step instead
# of re-downloading everything — much faster rebuilds.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the actual source code
COPY src ./src

# Build the JAR, skipping tests (tests should run in CI separately,
# not slow down every deployment)
RUN mvn clean package -DskipTests -B

# ---- STAGE 2: RUN ----
# Use a minimal JRE-only image — we don't need Maven or the JDK
# compiler at runtime, only the Java Runtime Environment
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy ONLY the built JAR from the build stage above
# (the --from=build flag pulls a file out of Stage 1's filesystem)
COPY --from=build /app/target/*.jar app.jar

# Render automatically sets the PORT environment variable.
# Spring Boot needs to listen on THAT port, not a hardcoded 8080.
# This EXPOSE is just documentation — the real binding happens
# via the SERVER_PORT environment variable in application.properties.
EXPOSE 8081

# Run the JAR.
# "java -jar app.jar" is how Spring Boot apps start in production.
ENTRYPOINT ["java", "-jar", "app.jar"]