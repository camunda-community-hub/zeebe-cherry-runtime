# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
EXPOSE 8080
COPY target/zeebe-cherry-runtime-*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar", "io.camunda.CherryApplication"]

