# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /app/uploads /app/logs && chown -R app /app
COPY --from=build /workspace/target/recipe-sharing-*.war app.war
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.war"]
