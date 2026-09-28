# ── Stage 1: Build ──────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy dependency descriptor first — Docker caches this layer;
# if pom.xml is unchanged, the download below is skipped entirely on rebuild.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Source changes far more often than pom.xml — copy it after deps
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# curl is only needed for the HEALTHCHECK below
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Never run as root in production
RUN addgroup --system spring && adduser --system --group spring
USER spring

# Copy ONLY the compiled JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]