FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/domain-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8082
EXPOSE 50051

ENTRYPOINT ["java", "-jar", "/app/app.jar"]