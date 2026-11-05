# syntax=docker/dockerfile:1
# Build context is this module directory:  docker build -t nexgen/payment-service nexgen-server/payment-service/payment-service
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY src/ src/
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 1001 --create-home appuser
COPY --from=build /workspace/target/payment-service-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8085
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
