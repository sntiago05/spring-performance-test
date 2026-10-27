# Build context: repository root.  docker build -f support-ticket-service/Dockerfile .

# ---- Build stage: Maven + JDK ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY support-ticket-service/pom.xml support-ticket-service/pom.xml
COPY technician-dispatch-service/pom.xml technician-dispatch-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl support-ticket-service -am dependency:go-offline -DskipTests || true
COPY support-ticket-service/src support-ticket-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl support-ticket-service -am package -DskipTests

# ---- Run stage: slim JRE ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/support-ticket-service/target/support-ticket-service.jar app.jar
USER app
ENV SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- "http://localhost:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
