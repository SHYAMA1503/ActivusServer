# Stage 1: Build with Maven using Java 23
FROM eclipse-temurin:23-jdk AS build

WORKDIR /app

# Copy source code
COPY . .

# Use Maven Wrapper to build the project
RUN chmod +x ./mvnw && ./mvnw clean install -DskipTests

# Stage 2: Run with JRE
FROM eclipse-temurin:23-jre

WORKDIR /app

# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
