# syntax=docker/dockerfile:1
# Build context is this module directory:  docker build -t nexgen/user-service nexgen-server/user-service/user-service
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY src/ src/
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Never run the service as root; the app only needs to read its jar and open a port.
RUN useradd --system --uid 1001 --create-home appuser
COPY --from=build /workspace/target/user-service-1.0.0.jar app.jar
USER appuser
EXPOSE 8081
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
