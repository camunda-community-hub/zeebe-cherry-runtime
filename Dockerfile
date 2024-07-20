# docker build -t zeebe-cherry:1.0.0 .
FROM openjdk:17-alpine
EXPOSE 9081
COPY target/zeebe-cherry-runtime-*-exec.jar /zeebe-cherry-runtime.jar
COPY pom.xml /pom.xml

# Create a directory
RUN mkdir -p /localstorage
RUN mkdir -p /localstorage/uploadpath
RUN mkdir -p /localstorage/classloader
WORKDIR  /

ENTRYPOINT ["java","-jar","/zeebe-cherry-runtime.jar"]

