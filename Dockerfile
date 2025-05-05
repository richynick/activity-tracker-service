FROM maven:3.8.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-jammy AS runner

WORKDIR /app

COPY --from=builder ./app/target/activity-tracker-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4000

ENTRYPOINT ["java", "-jar", "app.jar"]