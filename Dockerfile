FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle ./
RUN chmod +x gradlew
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
