# Stage 1: Build the application
# We use a Maven image to compile the code so you don't need Maven installed on your machine
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the jar, skipping tests to save time during container build
RUN mvn clean package -DskipTests

# Stage 2: Run the application
# We use a lightweight JDK image for the final container
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the RSA keys (Ensure these exist in your project root or adjust path)
# If you want to mount them as volumes instead (more secure), remove these COPY lines.
COPY keys/private_key_pkcs8.pem /app/keys/private_key_pkcs8.pem
COPY keys/public_key.pem /app/keys/public_key.pem

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
