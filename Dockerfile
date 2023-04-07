# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
# COPY target/zeebe-cherry-framework-*-jar-with-dependencies.jar app.jar


EXPOSE 8080
WORKDIR /app

# ARG JAR_FILE=target/*.jar
# COPY ${JAR_FILE} /app/runtime.jar

COPY target/zeebe-cherry-framework-*-jar-with-dependencies.jar /app/cherry.jar

ENTRYPOINT ["java","-jar","/app/cherry.jar", "io.camunda.CherryApplication"]