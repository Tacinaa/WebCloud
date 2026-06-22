FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY report-job/pom.xml report-job/
COPY report-job/src report-job/src/
RUN mvn -pl report-job -am package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/report-job/target/report-job.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
