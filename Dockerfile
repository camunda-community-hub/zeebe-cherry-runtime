
FROM eclipse-temurin:21-jre-alpine

EXPOSE 9081
COPY target/zeebe-cherry-runtime-*-jar-with-dependencies.jar /zeebe-cherry-runtime.jar
COPY pom.xml /pom.xml

# Create a directory
RUN mkdir -p /usr/local/cherry
RUN mkdir -p /usr/local/cherry/upload
RUN mkdir -p /usr/local/cherry/classloader


WORKDIR  /


ENTRYPOINT ["java","-jar","/zeebe-cherry-runtime.jar"]