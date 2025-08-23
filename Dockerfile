FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# List target directory contents for debugging
RUN ls -la /app/target/

# Runtime stage with Tomcat
FROM tomcat:10.1-jdk17

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the built WAR from build stage
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# Tomcat will automatically start
CMD ["catalina.sh", "run"]
