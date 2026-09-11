####
# ReLoop Quarkus backend (JVM mode)
# Build:  docker build -f backend-quarkus/Dockerfile -t reloop-backend-quarkus .
# (run from repo root so the backend-quarkus build context is available)
####

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend-quarkus/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend-quarkus/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quarkus-app /app/

EXPOSE 8080
ENV DB_HOST=reloop-postgres \
    DB_PORT=5432 \
    REDIS_HOST=reloop-redis \
    SMTP_HOST=reloop-mailpit \
    SMTP_PORT=1025

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
