FROM maven:3.8.3-openjdk-17 as builder
# setup base dir
WORKDIR /usr/src/app
# copy files from project
COPY pom.xml pom.xml
COPY src/ src/

# run maven build and cache dependencies
#RUN mvn dependency:resolve-plugins dependency:resolve clean package -DskipTests -Dhttps.protocols=TLSv1.1,TLSv1.2 --activate-profiles !default
RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests -Dmaven.test.skip clean package


# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
COPY --from=builder /usr/src/app/target/zeebe-cherry-framework-*-jar-with-dependencies.jar /usr/src/app/app.jar
ENTRYPOINT ["java","-jar","/usr/src/app/app.jar", "io.camunda.CherryApplication"]