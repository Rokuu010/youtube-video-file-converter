# Use a Maven image to build the application, specifying JDK 23
FROM maven:3.9.6-openjdk-23 AS build

# Set the working directory
WORKDIR /app

# Copy the project files into the container
COPY . .

# Package the application into a JAR file
RUN mvn clean package -DskipTests

# Use a smaller base image for the final application, based on JDK 23
FROM openjdk:23-jdk-slim

# Copy the JAR file from the build stage to the final image
COPY --from=build /app/target/*.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]