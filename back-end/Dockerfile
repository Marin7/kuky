# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Gradle wrapper and dependency descriptors first — this layer is reused
# as long as build files don't change, avoiding a full dependency re-download.
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Build the fat JAR — tests run in CI, not here.
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Extract Spring Boot layered JAR so each layer can be cached independently.
RUN java -Djarmode=layertools \
      -jar build/libs/back-end-0.0.1-SNAPSHOT.jar \
      extract --destination layers

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S kuky && adduser -S kuky -G kuky
WORKDIR /app

# Copy layers in order of least → most likely to change for optimal cache reuse.
COPY --from=builder /workspace/layers/dependencies/ ./
COPY --from=builder /workspace/layers/spring-boot-loader/ ./
COPY --from=builder /workspace/layers/snapshot-dependencies/ ./
COPY --from=builder /workspace/layers/application/ ./

USER kuky
EXPOSE 8081

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
