# Multi-stage Dockerfile for auth-service
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -B -q

COPY src/ src/
RUN ./mvnw package -DskipTests -B -q

# ---- Runtime image ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S tanalytics && adduser -S tanalytics -G tanalytics
USER tanalytics

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
