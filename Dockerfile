FROM  eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*cherry*.jar cherryapp.jar
ENTRYPOINT ["java","-Dspring.profiles.active=production", "-jar","/app/cherryapp.jar"]