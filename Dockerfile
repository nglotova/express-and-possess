# One build for both Java services. Pick the service with --target api or --target notifier.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY api/pom.xml api/
COPY notifier/pom.xml notifier/
RUN mvn --batch-mode --no-transfer-progress -pl api,notifier -am dependency:go-offline -q || true
COPY api api
COPY notifier notifier
RUN mvn --batch-mode --no-transfer-progress -pl api,notifier -am package -DskipTests

FROM eclipse-temurin:21-jre AS api
WORKDIR /app
RUN useradd --system --uid 10001 app && mkdir -p /app/uploads && chown app /app/uploads
COPY --from=build /src/api/target/api-*.jar app.jar
USER app
ENV UPLOADS_DIR=/app/uploads
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s \
  CMD curl -sf http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre AS notifier
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/notifier/target/notifier-*.jar app.jar
USER app
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s \
  CMD curl -sf http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
