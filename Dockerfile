# Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Debug: Print environment variables on startup
CMD echo "===== ENV DEBUG =====" && \
    echo "DB_HOST=$DB_HOST" && \
    echo "DB_PORT=$DB_PORT" && \
    echo "DB_NAME=$DB_NAME" && \
    echo "DB_USERNAME=$DB_USERNAME" && \
    echo "DB_PASSWORD is set: $([ -n \"$DB_PASSWORD\" ] && echo 'YES' || echo 'NO')" && \
    echo "=====================" && \
    java -jar app.jar
