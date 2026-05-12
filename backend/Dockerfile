# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies (cached layer)
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/auction-shop-1.0.0.jar app.jar

# Expose the port defined in application.yml
EXPOSE 1234

ENTRYPOINT ["java", "-jar", "app.jar"]
