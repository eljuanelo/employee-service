FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/employee-service-*.jar /app/employee-service.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/employee-service.jar"]
