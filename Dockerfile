# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
EXPOSE 9081
COPY target/zeebe-cherry-runtime-3.0.0-exec.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

