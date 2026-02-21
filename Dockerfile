# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY frontend/package.json frontend/package-lock.json ./frontend/

RUN --mount=type=cache,target=/root/.m2 \
    MAVEN_CONFIG="" ./mvnw -B -ntp -Dmaven.repo.local=/root/.m2/repository -DskipTests dependency:go-offline

COPY src ./src
COPY frontend ./frontend

RUN --mount=type=cache,target=/root/.m2 \
    MAVEN_CONFIG="" ./mvnw -B -ntp -Dmaven.repo.local=/root/.m2/repository clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/snayvik-ct-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
