
FROM eclipse-temurin:23-jre-alpine

EXPOSE 9081

WORKDIR /usr/local/cherry

COPY target/zeebe-cherry-runtime-*-jar-with-dependencies.jar /usr/local/cherry/zeebe-cherry-runtime.jar
# getVersion() in RuntimeRestController reads this as a relative path - must sit next to the JAR.
COPY pom.xml /usr/local/cherry/pom.xml

# Match the default (relative) paths in application.yaml: cherry.connectorslib.uploadpath and
# .classloaderpath are "./localstorage/upload" and "./localstorage/classloader" - resolved from
# WORKDIR, so a deployment with no CHERRY_CONNECTORSLIB_* override still has somewhere to write.
RUN mkdir -p /usr/local/cherry/localstorage/upload
RUN mkdir -p /usr/local/cherry/localstorage/classloader

ENTRYPOINT ["java","-jar","/usr/local/cherry/zeebe-cherry-runtime.jar"]