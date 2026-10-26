FROM maven:3.9.13-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
RUN useradd -r -u 10001 mirror
WORKDIR /app
COPY --from=build /app/target/mirror-self-0.1.0-SNAPSHOT.jar /app/app.jar
USER mirror
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70.0","-jar","/app/app.jar"]
