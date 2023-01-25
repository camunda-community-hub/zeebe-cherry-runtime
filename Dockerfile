# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
COPY target/zeebe-cherry-framework-*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar", "io.camunda.CherryApplication"]