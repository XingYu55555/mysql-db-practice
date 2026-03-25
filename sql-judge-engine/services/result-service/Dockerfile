FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/result-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
