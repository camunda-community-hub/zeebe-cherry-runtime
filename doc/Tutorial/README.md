# Tutorial

## Introduction
the purpose of this document is to explain how to develop a simple example with the runtime. The target audience are the developers.

Just create a simple hello word project

This project explains how to create a simple "Hello Word" worker, and execute it via the Cherry Framework

You can create your own project following these different steps.

This tutorial is completely available here
[Simple Example](https://github.com/pierre-yves-monnet/zeebe-cherry-simpleexample)


# Maven.xml

To include the Cherry Framework, add in your pom.xml this library

`````xml
  <dependency>
      <groupId>io.camunda.community</groupId>
      <artifactId>zeebe-cherry-runtime</artifactId>
      <version>${cherry.version}</version>
  </dependency>


`````

You need to add the Connector SDK and Zeebe Client.

`````xml
   <dependency>
      <groupId>io.camunda</groupId>
      <artifactId>spring-zeebe-starter</artifactId>
      <version>${zeebe.version}</version>
    </dependency>
    <dependency>
      <groupId>io.camunda</groupId>
      <artifactId>zeebe-client-java</artifactId>
      <version>${zeebe-client.version}</version>
    </dependency>

    <!-- Accept Camunda Connector -->
    <dependency>
      <groupId>io.camunda.connector</groupId>
      <artifactId>connector-core</artifactId>
      <version>${connector-core.version}</version>
    </dependency>
    <dependency>
      <groupId>io.camunda.connector</groupId>
      <artifactId>connector-validation</artifactId>
      <version>${connector-validation.version}</version>
    </dependency>
`````

Do a
````
mvn install
````

to retrieve all libraries

# The application.yaml

The project is a SpringBoot application. Configuration can be set in the `src/main/resources/application.yaml` file

Define the connection to the ZeebeEngine

`````yaml
zeebe.client:
broker.gateway-address: 127.0.0.1:26500
# zeebe.client.broker.gateway-address=host.docker.internal:26500
security.plaintext: true

# use a cloud Zeebe engine
# zeebe.client.cloud.region=
# zeebe.client.cloud.clusterId=
# zeebe.client.cloud.clientId=
# zeebe.client.cloud.clientSecret=

`````

The Cherry Runtime uses a database to save statistics and status.
By default, an H2 database is configured.


````yaml

# Database
spring.datasource:
  url: "jdbc:h2:file:./cherry.db"
  driver-class-name: "org.h2.Driver"
  username: "sa"
  password: "password"
spring.jpa:
  hibernate.ddl-auto: update
  generate-ddl: true
  database-platform: "org.hibernate.dialect.H2Dialect"
````


You can use any other SpringBoot variable, for example

`````yaml
server.port: 9091
`````


Note: if the connection to a Zeebe Server is not provided in the configuration, then the Cherry Runtime will ask the administrator to give the information in the UI.


# Your first Worker

Define a new class. Let's review each part of the class.

## Declaration

Define a new class. This class extends AbstractWorker

`````java
package io.camunda.cherry.helloword;


@Component
public class HelloWordWorker extends AbstractWorker 

`````

Note:

1. Your class must declare @Component ou @Service. This notation is used by Cherry to detect the different connector and worker
2. You can choose between AbstractWorker and AbstractConnector. In this example, AbstractWorker is choosen. Check the Developper manual to see the difference


## Constructor

In the constructor, you have to specify
`````java
  public HelloWordWorker() {
    super( // Type
    "helloword",
    // List of Input
    Arrays.asList(
    RunnerParameter.getInstance(INPUT_COUNTRY, "Country name", String.class, RunnerParameter.Level.OPTIONAL,
    "Country to whom to say hello")
    .addChoice(COUNTRY_V_USA, "United State of America")
    .addChoice(COUNTRY_V_GE, "Germany")
    .addChoice(COUNTRY_V_FR, "France")
    .addChoice(COUNTRY_V_EN, "England")
    .addChoice(COUNTRY_V_IT, "Italy")
    .addChoice(COUNTRY_V_SP, "Spain"),

    RunnerParameter.getInstance(INPUT_STATE, "State", String.class, RunnerParameter.Level.OPTIONAL, "State")
    .addCondition(INPUT_COUNTRY, Collections.singletonList(COUNTRY_V_USA))),

    // list of Output
    Collections.singletonList(
    RunnerParameter.getInstance(OUTPUT_MESSAGE, OUTPUT_MESSAGE, String.class, RunnerParameter.Level.REQUIRED,
    "Welcome Message")),
    // List of BPMN Error
    Collections.singletonList(
    BpmnError.getInstance(BPMN_ERROR_NOTIME, "Sorry, no time to say hello now, I'm busy")));
    }


`````

* The type: this is the type of workers used in the Element-Template

* The list of Inputs

The Runtime uses the list of Inputs to calculate the Element-Template. The second advantage is to reduce your verification.
For example, when you define an Input as REQUIRED, the Runtime will check the existence of this parameter. If the parameter does not exist
the Runtime will throw an error and will not call your method.

See below for a detailed explanation.

* The list of Outputs

Declare this information for the documentation. Attention, if the output is declared as REQUIRED, then the Runtime will verify that you provided the information.
If not, it will throw an error (the worker does not respect the contract).

* The list of Errors

Declare the list of Errors. There are multiple impacts. The first one is the documentation: this list will be visible. The second one is the Element-Template.
When the worker throws a ConnectorExecption, then the ZeebeClient, according to the definition in the Element-Template, will throw a BPMN Error.
Else, the ConnectorException will send a Fail Task.

## Define an Input
The input contains different part
RunnerParameter.getInstance(INPUT_COUNTRY, "Country name", String.class, RunnerParameter.Level.OPTIONAL,
"Country to whom to say hello")

The code. This is the value expected from the worker.
The label. Labels are visible  in the documentation and in the Element-template
The class (String.class here). This is nice for the documentation, and the Runtime checks the type.
Level (OPTIONAL/REQUIRED). A REQUIRED input must be provided by the process.
A description for the documentation and as a hint in the Element-template

## Add Input decorator
Multiple decorators exist for inputs.
AddChoice give a list of choice, then a Select Box is created in the Element-tempalte

    .addChoice(COUNTRY_V_USA, "United State of America")
    .addChoice(COUNTRY_V_GE, "Germany")
    .addChoice(COUNTRY_V_FR, "France")
    .addChoice(COUNTRY_V_EN, "England")
    .addChoice(COUNTRY_V_IT, "Italy")
    .addChoice(COUNTRY_V_SP, "Spain"),


Condition describes a show/hide condition, exploitable in the Element-template
.addCondition(INPUT_COUNTRY, Collections.singletonList(COUNTRY_V_USA))),

Check the documentation for the list of decorator.

## The different information

Different methods, like getName(), getCollectionName(), getDescription(), getLogo() can be overridden in order to give more explanation to your worker.

## The execute method
This is the core of your work!
In the method you have different methods to simplify your work.
getInput...Value() : multiple getInput methods simplify your code. For example, getInputStringValue() returns a String.

Note: if you use a Connector, all Inputs are grouped under an object. This simplifies the approach, except that you have to define an extra class.

The setOuputValue() simplifies the way to produce the result. Attention: any REQUIRED output must be resolved by a call to the setOuputValue() method: this is the implementation for the framework to check your contract.

## How to throw an error?

Just throw a ConnectorException(). If the worker declare some BPMN Error, then this will be transformed by the Connector SDK to a BPMN Error.

# Execute in an IDE

In the IDE like Intellij, the class to start is `io.camunda.CherryApplication`
![Intellij](doc/IntellijConfiguration.png?raw=true)

Execute this project

The Runtime started
````
"C:\Program Files\Java\jdk-17.0.3.1\bin\java.exe" -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:59003,suspend=y,server=n -javaagent:C:\Users\Pierre-YvesMonnet\AppData\Local\JetBrains\IntelliJIdea2021.3\captureAgent\debugger-agent.jar=file:/C:/Users/Pierre-YvesMonnet/AppData/Local/Temp/capture.props -Dfile.encoding=UTF-8 -classpath "D:\dev\intellij\community\cherry\zeebe-cherry-simpleexample\target\classes;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-web\2.7.4\spring-boot-starter-web-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter\2.7.4\spring-boot-starter-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot\2.7.4\spring-boot-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\2.7.4\spring-boot-autoconfigure-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-logging\2.7.4\spring-boot-starter-logging-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\ch\qos\logback\logback-classic\1.2.11\logback-classic-1.2.11.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\ch\qos\logback\logback-core\1.2.11\logback-core-1.2.11.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.17.2\log4j-to-slf4j-2.17.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\logging\log4j\log4j-api\2.17.2\log4j-api-2.17.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\slf4j\jul-to-slf4j\1.7.36\jul-to-slf4j-1.7.36.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\jakarta\annotation\jakarta.annotation-api\1.3.5\jakarta.annotation-api-1.3.5.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-core\5.3.23\spring-core-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-jcl\5.3.23\spring-jcl-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\yaml\snakeyaml\1.30\snakeyaml-1.30.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-json\2.7.4\spring-boot-starter-json-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jdk8\2.13.4\jackson-datatype-jdk8-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\module\jackson-module-parameter-names\2.13.4\jackson-module-parameter-names-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\2.7.4\spring-boot-starter-tomcat-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\9.0.65\tomcat-embed-core-9.0.65.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\9.0.65\tomcat-embed-el-9.0.65.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\9.0.65\tomcat-embed-websocket-9.0.65.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-web\5.3.23\spring-web-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-beans\5.3.23\spring-beans-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-webmvc\5.3.23\spring-webmvc-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-aop\5.3.23\spring-aop-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-context\5.3.23\spring-context-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-expression\5.3.23\spring-expression-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\spring\spring-boot-starter-camunda\8.2.0\spring-boot-starter-camunda-8.2.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\spring\spring-client-zeebe\8.2.0\spring-client-zeebe-8.2.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\spring\spring-client-common\8.2.0\spring-client-common-8.2.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\spring\spring-client-annotations\8.2.0\spring-client-annotations-8.2.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\connector\connector-runtime-util\0.8.0\connector-runtime-util-0.8.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\camunda\feel\feel-engine\1.16.0\feel-engine-1.16.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\scala-lang\scala-library\2.13.10\scala-library-2.13.10.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\lihaoyi\fastparse_2.13\2.3.3\fastparse_2.13-2.3.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\lihaoyi\sourcecode_2.13\0.2.3\sourcecode_2.13-0.2.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\lihaoyi\geny_2.13\0.6.10\geny_2.13-0.6.10.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\module\jackson-module-scala_3\2.13.4\jackson-module-scala_3-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\scala-lang\scala3-library_3\3.0.2\scala3-library_3-3.0.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\thoughtworks\paranamer\paranamer\2.8\paranamer-2.8.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\commons-beanutils\commons-beanutils\1.9.4\commons-beanutils-1.9.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\commons-collections\commons-collections\3.2.2\commons-collections-3.2.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\camunda-operate-client-java\8.1.7.2\camunda-operate-client-java-8.1.7.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\httpcomponents\client5\httpclient5\5.1.3\httpclient5-5.1.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\httpcomponents\core5\httpcore5\5.1.4\httpcore5-5.1.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\httpcomponents\core5\httpcore5-h2\5.1.4\httpcore5-h2-5.1.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\commons-codec\commons-codec\1.15\commons-codec-1.15.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\httpcomponents\client5\httpclient5-fluent\5.1.3\httpclient5-fluent-5.1.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\github\resilience4j\resilience4j-retry\2.0.2\resilience4j-retry-2.0.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\github\resilience4j\resilience4j-core\2.0.2\resilience4j-core-2.0.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\zeebe-client-java\8.2.3\zeebe-client-java-8.2.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\zeebe-bpmn-model\8.2.3\zeebe-bpmn-model-8.2.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\camunda\bpm\model\camunda-xml-model\7.18.0\camunda-xml-model-7.18.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.13.4\jackson-core-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.13.4\jackson-databind-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.13.4\jackson-annotations-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\zeebe-gateway-protocol-impl\8.2.3\zeebe-gateway-protocol-impl-8.2.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-protobuf\1.54.1\grpc-protobuf-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\api\grpc\proto-google-common-protos\2.9.0\proto-google-common-protos-2.9.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-protobuf-lite\1.54.1\grpc-protobuf-lite-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\guava\guava\31.1-jre\guava-31.1-jre.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\guava\failureaccess\1.0.1\failureaccess-1.0.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\guava\listenablefuture\9999.0-empty-to-avoid-conflict-with-guava\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\j2objc\j2objc-annotations\1.3\j2objc-annotations-1.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-stub\1.54.1\grpc-stub-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\errorprone\error_prone_annotations\2.18.0\error_prone_annotations-2.18.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-core\1.54.1\grpc-core-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\android\annotations\4.1.1.4\annotations-4.1.1.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\codehaus\mojo\animal-sniffer-annotations\1.21\animal-sniffer-annotations-1.21.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\perfmark\perfmark-api\0.25.0\perfmark-api-0.25.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-api\1.54.1\grpc-api-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-context\1.54.1\grpc-context-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\grpc\grpc-netty\1.54.1\grpc-netty-1.54.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-codec-http2\4.1.82.Final\netty-codec-http2-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-codec-http\4.1.82.Final\netty-codec-http-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-handler-proxy\4.1.82.Final\netty-handler-proxy-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-codec-socks\4.1.82.Final\netty-codec-socks-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-transport-native-unix-common\4.1.82.Final\netty-transport-native-unix-common-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\protobuf\protobuf-java\3.22.3\protobuf-java-3.22.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-boringssl-static\2.0.54.Final\netty-tcnative-boringssl-static-2.0.54.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-classes\2.0.54.Final\netty-tcnative-classes-2.0.54.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-boringssl-static\2.0.54.Final\netty-tcnative-boringssl-static-2.0.54.Final-linux-x86_64.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-boringssl-static\2.0.54.Final\netty-tcnative-boringssl-static-2.0.54.Final-linux-aarch_64.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-boringssl-static\2.0.54.Final\netty-tcnative-boringssl-static-2.0.54.Final-osx-x86_64.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-boringssl-static\2.0.54.Final\netty-tcnative-boringssl-static-2.0.54.Final-osx-aarch_64.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-tcnative-boringssl-static\2.0.54.Final\netty-tcnative-boringssl-static-2.0.54.Final-windows-x86_64.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-handler\4.1.82.Final\netty-handler-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-resolver\4.1.82.Final\netty-resolver-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-buffer\4.1.82.Final\netty-buffer-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-transport\4.1.82.Final\netty-transport-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-codec\4.1.82.Final\netty-codec-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\netty\netty-common\4.1.82.Final\netty-common-4.1.82.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.13.4\jackson-dataformat-yaml-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\connector\connector-core\0.8.1\connector-core-0.8.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\connector\connector-validation\0.8.1\connector-validation-0.8.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\jakarta\validation\jakarta.validation-api\2.0.2\jakarta.validation-api-2.0.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\hibernate\validator\hibernate-validator\6.2.5.Final\hibernate-validator-6.2.5.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\jboss\logging\jboss-logging\3.4.3.Final\jboss-logging-3.4.3.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\classmate\1.5.1\classmate-1.5.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\community\zeebe-cherry-runtime\3.0.0\zeebe-cherry-runtime-3.0.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\filestorage\filestorage\1.1.0\filestorage-1.1.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.13.4\jackson-datatype-jsr310-2.13.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\google\code\gson\gson\2.9.1\gson-2.9.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-data-jpa\2.7.4\spring-boot-starter-data-jpa-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-aop\2.7.4\spring-boot-starter-aop-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\aspectj\aspectjweaver\1.9.7\aspectjweaver-1.9.7.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\2.7.4\spring-boot-starter-jdbc-2.7.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\zaxxer\HikariCP\4.0.3\HikariCP-4.0.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-jdbc\5.3.23\spring-jdbc-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\jakarta\transaction\jakarta.transaction-api\1.3.3\jakarta.transaction-api-1.3.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\jakarta\persistence\jakarta.persistence-api\2.2.3\jakarta.persistence-api-2.2.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\hibernate\hibernate-core\5.6.11.Final\hibernate-core-5.6.11.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\net\bytebuddy\byte-buddy\1.12.17\byte-buddy-1.12.17.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\antlr\antlr\2.7.7\antlr-2.7.7.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\jboss\jandex\2.4.2.Final\jandex-2.4.2.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\hibernate\common\hibernate-commons-annotations\5.1.2.Final\hibernate-commons-annotations-5.1.2.Final.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\glassfish\jaxb\jaxb-runtime\2.3.6\jaxb-runtime-2.3.6.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\2.3.3\jakarta.xml.bind-api-2.3.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\glassfish\jaxb\txw2\2.3.6\txw2-2.3.6.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\sun\istack\istack-commons-runtime\3.0.12\istack-commons-runtime-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\sun\activation\jakarta.activation\1.2.2\jakarta.activation-1.2.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\data\spring-data-jpa\2.7.3\spring-data-jpa-2.7.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\data\spring-data-commons\2.7.3\spring-data-commons-2.7.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-orm\5.3.23\spring-orm-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-tx\5.3.23\spring-tx-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\springframework\spring-aspects\5.3.23\spring-aspects-5.3.23.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\postgresql\postgresql\42.3.7\postgresql-42.3.7.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\checkerframework\checker-qual\3.5.0\checker-qual-3.5.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\chemistry\opencmis\chemistry-opencmis-client-impl\1.1.0\chemistry-opencmis-client-impl-1.1.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\chemistry\opencmis\chemistry-opencmis-client-api\1.1.0\chemistry-opencmis-client-api-1.1.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\chemistry\opencmis\chemistry-opencmis-commons-api\1.1.0\chemistry-opencmis-commons-api-1.1.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\chemistry\opencmis\chemistry-opencmis-commons-impl\1.1.0\chemistry-opencmis-commons-impl-1.1.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\codehaus\woodstox\woodstox-core-asl\4.4.1\woodstox-core-asl-4.4.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\codehaus\woodstox\stax2-api\3.1.4\stax2-api-3.1.4.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\chemistry\opencmis\chemistry-opencmis-client-bindings\1.1.0\chemistry-opencmis-client-bindings-1.1.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-frontend-jaxws\3.0.12\cxf-rt-frontend-jaxws-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\xml-resolver\xml-resolver\1.2\xml-resolver-1.2.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\asm\asm\3.3.1\asm-3.3.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-core\3.0.12\cxf-core-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\ws\xmlschema\xmlschema-core\2.2.1\xmlschema-core-2.2.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-bindings-soap\3.0.12\cxf-rt-bindings-soap-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-wsdl\3.0.12\cxf-rt-wsdl-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-databinding-jaxb\3.0.12\cxf-rt-databinding-jaxb-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\sun\xml\bind\jaxb-impl\2.1.14\jaxb-impl-2.1.14.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\sun\xml\fastinfoset\FastInfoset\1.2.12\FastInfoset-1.2.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\com\sun\xml\bind\jaxb-core\2.1.14\jaxb-core-2.1.14.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\javax\xml\bind\jaxb-api\2.3.1\jaxb-api-2.3.1.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\javax\activation\javax.activation-api\1.2.0\javax.activation-api-1.2.0.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-bindings-xml\3.0.12\cxf-rt-bindings-xml-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-frontend-simple\3.0.12\cxf-rt-frontend-simple-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-ws-addr\3.0.12\cxf-rt-ws-addr-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-transports-http\3.0.12\cxf-rt-transports-http-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\cxf\cxf-rt-ws-policy\3.0.12\cxf-rt-ws-policy-3.0.12.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\wsdl4j\wsdl4j\1.6.3\wsdl4j-1.6.3.jar;C:\Users\Pierre-YvesMonnet\.m2\repository\org\apache\neethi\neethi\3.0.3\neethi-3.0.3.jar;D:\atelier\IntelliJ IDEA 2021.3.1\lib\idea_rt.jar" io.camunda.CherryApplication
Connected to the target VM, address: '127.0.0.1:59003', transport: 'socket'

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.7.4)

2023-05-01 17:38:08.148  INFO 35564 --- [           main] io.camunda.CherryApplication             : Starting CherryApplication v3.0.0 using Java 17.0.3.1 on LAPTOP-B6HDDE9H with PID 35564 (C:\Users\Pierre-YvesMonnet\.m2\repository\io\camunda\community\zeebe-cherry-runtime\3.0.0\zeebe-cherry-runtime-3.0.0.jar started by Pierre-YvesMonnet in D:\dev\intellij\community\cherry\zeebe-cherry-simpleexample)
2023-05-01 17:38:08.154  INFO 35564 --- [           main] io.camunda.CherryApplication             : No active profile set, falling back to 1 default profile: "default"
2023-05-01 17:38:15.551  INFO 35564 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2023-05-01 17:38:16.291  INFO 35564 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 711 ms. Found 5 JPA repository interfaces.

.....


2023-05-01 17:38:26.347  INFO 35564 --- [           main] io.camunda.CherryApplication             : Started CherryApplication in 21.314 seconds (JVM running for 24.944)
````

The UI is available via the URL `http://localhost:9091` according the value in the `application.yaml` file

![Cherry Dashboard](CherryDashboard.png?raw=true)

To view only the worker in the dashboard, unselect the toggle "Framework runner"
![Unselect Framework Runner](UnselectFrameworkRunner.png?raw=true)

Only the worker is part of the list
![Cherry Dashboard Filtered](CherryDashboardOnlyWorker.png?raw=true)

# Generate a Docker image

Thanks to Spring, the command generate a Docker image.

## Generate the image
The project contain a Dockerfile

`````
# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
EXPOSE 8080
COPY target/zeebe-cherry-simpleexample-*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar", "io.camunda.CherryApplication"]
`````

To generate the Docker image, execute
````
mvn install
docker build -t zeebe-cherry-simpleexample:1.0.0 .
````

Run it with
````
docker run -p 8888:9091 zeebe-cherry-simpleexample:1.0.0 .
````
Note: the 9091 is part of the `application.yaml` file. If you change the port number, change it here.

# An execution
To verify the execution, check the process `src/main/resources/SayHello.bpmn`

![Say Hello process](SayHelloProcess.png?raw=true)

This process execute the worker.

Deploy it in your Cluster.
Adapt the `application.yaml` to connect to your Zeebe cluster.
Create a process instance, for the Modeler for example.

Access the dashboard. You see that one execution was processed

![Cherry Dashboard](OneExecutionDashboard.png?raw=true)

In Operation, the execution is visible
![Cherry Operation](OneExecutionOperation.png?raw=true)

Accessing Operate, the result is visible too, and the variable is calculated
![Operate](OneExecutionOperate.png?raw=true)

# Reference

## Process
 
Load the process here
[SayHello.bpmn](SayHello.bpmn)

## pom.xml

Location: `/pom.xml`

`````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>groupId</groupId>
  <artifactId>zeebe-cherry-simpleexample</artifactId>
  <version>1.0</version>

  <properties>
    <java.version>17</java.version>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <maven.compiler.source>${java.version}</maven.compiler.source>

    <zeebe.version>8.2.0</zeebe.version>
    <zeebe-client.version>8.2.3</zeebe-client.version>

    <connector-core.version>0.8.1</connector-core.version>
    <connector-validation.version>0.8.1</connector-validation.version>

    <cherry.version>3.0.0</cherry.version>
    <spring.boot.version>2.7.4</spring.boot.version>

  </properties>


  <dependencyManagement>
    <dependencies>
      <dependency>
        <!-- Import dependency management from Spring Boot -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>


  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <version>2.7.4</version>
    </dependency>

    <dependency>
      <groupId>io.camunda</groupId>
      <artifactId>spring-zeebe-starter</artifactId>
      <version>${zeebe.version}</version>
    </dependency>
    <dependency>
      <groupId>io.camunda</groupId>
      <artifactId>zeebe-client-java</artifactId>
      <version>${zeebe-client.version}</version>
    </dependency>

    <!-- Accept Camunda Connector -->
    <dependency>
      <groupId>io.camunda.connector</groupId>
      <artifactId>connector-core</artifactId>
      <version>${connector-core.version}</version>
    </dependency>
    <dependency>
      <groupId>io.camunda.connector</groupId>
      <artifactId>connector-validation</artifactId>
      <version>${connector-validation.version}</version>
    </dependency>

    <dependency>
      <groupId>io.camunda.community</groupId>
      <artifactId>zeebe-cherry-runtime</artifactId>
      <version>${cherry.version}</version>
    </dependency>
  </dependencies>



  <build>

    <plugins>
      <!-- allow mvn spring-boot:run -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>2.7.5</version>
        <configuration>
          <mainClass>io.camunda.CherryApplication</mainClass>
        </configuration>
      </plugin>

      <!-- build the jar with all dependencies -->
      <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <executions>
          <execution>
            <phase>install</phase>
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
          </descriptorRefs>
          <archive>
            <manifest>
              <mainClass>io.camunda.CherryApplication</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>

    </plugins>
  </build>

</project>
`````

## Application.yaml

Location: `/src/main/resources/application.yaml`

`````yaml
# use a OnPremise Zeebe engine
zeebe.client:
  broker.gateway-address: 127.0.0.1:26500
  # zeebe.client.broker.gateway-address=host.docker.internal:26500
  security.plaintext: true

  # use a cloud Zeebe engine
  # zeebe.client.cloud.region=
  # zeebe.client.cloud.clusterId=
  # zeebe.client.cloud.clientId=
  # zeebe.client.cloud.clientSecret=


server.port: 9091

# Database
spring.datasource:
  url: "jdbc:h2:file:./cherry.db"
  driver-class-name: "org.h2.Driver"
  username: "sa"
  password: "password"
spring.jpa.database-platform: "org.hibernate.dialect.H2Dialect"
org.hibernate.dialect: H2Dialect



spring.jpa:
  generate-ddl: true
  hibernate.ddl-auto: "update"

  properties:
    hibernate:
      format_sql: true
      show-sql: true

`````

## HelloWordWorker

Location: `src/main/java/io/camunda/cherry/helloword/HelloWordWorker.java`

`````java
/* ******************************************************************** */
/*                                                                      */
/*  GenerateOfficeWorker                                                */
/*                                                                      */
/*  Generate a office document (Microsoft, OpenOffice) from a template  */
/*  and a dictionary ti replace the place holder in the document        */
/* ******************************************************************** */
package io.camunda.cherry.helloword;

import io.camunda.cherry.definition.AbstractWorker;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

@Component
public class HelloWordWorker extends AbstractWorker {

  public static final String LOGO = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='18' height='18.0' viewBox='0 0 18 18.0' %3E%3Cg id='XMLID_238_'%3E %3Cpath id='XMLID_239_' d='m 14.708846 10.342394 c -1.122852 0.0,-2.528071 0.195852,-2.987768 0.264774 C 9.818362 8.6202,9.277026 7.4907875,9.155265 7.189665 C 9.320285 6.765678,9.894426 5.155026,9.976007 3.0864196 C 10.016246 2.0507226,9.797459 1.2768387,9.325568 0.7862517 C 8.854491 0.29647747,8.284297 0.2583872,8.120788 0.2583872 c -0.573329 0.0,-1.5351098 0.28991616,-1.5351098 2.2313614 c 0.0 1.6845098,0.7853807 3.4719677,1.0024838 3.933813 C 6.444349 9.754026,5.216342 12.03393,4.9555745 12.502801 C 0.35941938 14.233297,0.0 15.906485,0.0 16.380697 c 0.0 0.852155,0.6068903 1.360916,1.6234258 1.360916 c 2.4697742 0.0,4.7236066 -4.146503,5.096265 -4.866503 c 1.754129 -0.698923,4.101909 -1.131852,4.6987553 -1.235148 c 1.711974 1.6308,3.691916 2.065935,4.514109 2.065935 c 0.61862 0.0,2.067387 0.0,2.067387 -1.489529 C 18.0 10.833156,16.227118 10.342394,14.708846 10.342394 m -0.119033 0.977865 c 1.334032 0.0,1.6866 0.441174,1.6866 0.674419 c 0.0 0.146381,-0.05557 0.623962,-0.770632 0.623962 c -0.641207 0.0,-1.748265 -0.370568,-2.837497 -1.174646 c 0.454238 -0.05969,1.126394 -0.123735,1.921529 -0.123735 M 8.050761 1.2062323 c 0.1216452 0.0,0.2017161 0.039077,0.2677355 0.1306452 C 8.702187 1.8692712,8.392819 3.6088843,8.016271 4.9702067 C 7.6527867 3.8029358,7.3799996 2.0118778,7.7638063 1.3817617 C 7.838826 1.2587807,7.9245877 1.2062323,8.050761 1.2062323 M 7.402878 11.626084 C 7.885859 10.650368,8.427195 9.228368,8.722046 8.424116 C 9.312098 9.411736,10.105723 10.32869,10.564724 10.825839 C 9.135581 11.127136,8.054303 11.428143,7.402878 11.626084 m -6.443478 4.884794 c -0.0318194 -0.03774,-0.0365226 -0.11729,-0.0125419 -0.212806 c 0.0502839 -0.200149,0.4345548 -1.192297,3.2139292 -2.435575 c -0.3979743 0.626865,-1.0201355 1.522568,-1.703613 2.191704 c -0.4811226 0.450348,-0.8557549 0.678716,-1.1135033 0.678716 c -0.092206 1.0E-6,-0.2192516 -0.02514,-0.384271 -0.222039 z'/%3E%3C/g%3E%3Cpath fill='%23AA0000' style='stroke-width:0.414187' id='path49' d='M 17.801788 5.406512 C 17.740654 6.1878333,17.117386 6.562838,16.990644 6.6314273 C 16.315935 6.999722,15.180484 6.87149,14.63177 6.0991144 C 14.329082 5.674159,14.176993 4.997958,14.471479 4.529762 l 0.0037 -0.00746 C 14.581045 4.350083,14.826327 4.059324,15.278867 4.049632 h 0.0164 c 0.161781 0.0,0.348165 0.049205,0.512928 0.092446 c 0.115558 0.030567,0.21546 0.056661,0.290014 0.061879 c 0.04697 0.00298,0.114067 -0.00895,0.199058 -0.023857 c 0.254227 -0.044732,0.678437 -0.1192857,1.049714 0.1431429 c 0.505473 0.3586031,0.457759 1.0541877,0.454777 1.0832635 z'/%3E%3Cpath fill='%23AA0000' style='stroke-width:0.414187' id='path51' d='m 14.711542 4.1592307 c -0.145379 0.1028839,-0.241554 0.2303706,-0.299705 0.3243081 l -0.0037 0.00596 c -0.150598 0.2400625,-0.192348 0.5188929,-0.164018 0.791759 c -0.003 0.00149,-0.006 0.00298,-0.0082 0.00522 c -0.552442 0.4510491,-1.176455 0.3347455,-1.418755 0.2639196 l -0.01417 -0.00373 C 12.03134 5.354325,11.269403 4.520816,11.409564 3.6328826 C 11.487104 3.143811,11.856885 2.581677,12.414546 2.4534447 c 0.09617 -0.022366,0.598665 -0.115558,1.002746 0.2229152 c 0.123759 0.1043751,0.21546 0.2564644,0.295977 0.3906608 c 0.05741 0.09692,0.107358 0.1796741,0.161782 0.2340983 c 0.03504 0.03504,0.09617 0.069335,0.17371 0.1133214 c 0.227388 0.1274867,0.570335 0.3205804,0.659799 0.7365894 c 7.45E-4 0.00298,0.0015 0.00596,0.003 0.0082 z'/%3E%3Cpath fill='%23502D16' style='stroke-width:0.414187' id='path53' d='m 16.22647 4.1174803 c -0.04399 0.00746,-0.08275 0.012674,-0.110339 0.012674 c -0.0045 0.0,-0.0089 -7.456E-4,-0.01267 -7.456E-4 c -0.02088 -0.00149,-0.04548 -0.00447,-0.07157 -0.00969 c 0.228879 -0.5845001,0.323563 -1.519402,-0.471924 -2.7033129 c -0.0015 -0.00298,-0.0037 -0.00522,-0.0067 -0.00746 c 0.04622 -0.1834018,0.06188 -0.311634,0.06561 -0.3429465 c 0.0022 -0.020875,-0.01267 -0.038768,-0.0328 -0.041004 c -0.02088 -0.00298,-0.03877 0.012674,-0.04101 0.032804 c -0.0089 0.079772,-0.09841 0.7992145,-0.600156 1.4381388 c -0.313871 0.3996073,-0.808161 0.6970761,-0.978889 0.7820671 c -0.01566 -0.010438,-0.02908 -0.020875,-0.03802 -0.029821 c -0.0246 -0.024603,-0.04846 -0.056661,-0.07306 -0.094683 c 0.01417 -0.00746,0.02908 -0.015656,0.04473 -0.023857 c 0.178183 -0.095429,0.476398 -0.2542278,0.840219 -0.6351966 c 0.430175 -0.4503036,0.512184 -1.1399243,0.391407 -1.3277993 c -0.05815 -0.090955,-0.151344 -0.099902,-0.225898 -0.1066116 c -0.09767 -0.0082,-0.156562 -0.014165,-0.167 -0.14687057 c -0.0097 -0.11555806,0.04473 -0.21769646,0.152835 -0.28628576 c 0.14538 -0.093192,0.405572 -0.12301341,0.657563 0.0201295 c 0.157308 0.0887188,0.155817 0.24006253,0.155071 0.38618752 c -7.45E-4 0.097665,-0.0015 0.1901116,0.04697 0.2564643 l 0.02088 0.02833 c 0.119286 0.1617813,0.398116 0.5390224,0.565116 1.1973305 c 0.178183 0.7045312,0.0097 1.3464376,-0.110339 1.6021564 z'/%3E%3Cpath style='stroke-width:0.414187' id='path55' d='m 14.033104 1.060038 c -0.006 0.00298,-0.01193 0.00447,-0.01789 0.00447 c -0.01342 0.0,-0.02609 -0.00745,-0.0328 -0.019384 c -7.46E-4 -7.455E-4,-0.05293 -0.0939375,-0.146871 -0.1796741 c -0.01566 -0.0134196,-0.0164 -0.0372768,-0.003 -0.0521875 c 0.01417 -0.0156563,0.03728 -0.0164018,0.05293 -0.002982 c 0.10363 0.0939375,0.159545 0.1945848,0.161782 0.1990581 c 0.01044 0.017893,0.0037 0.040259,-0.01417 0.050696 z'/%3E%3Cpath style='stroke-width:0.414187' id='path57' d='m 14.108404 1.7705336 c -0.07754 0.091701,-0.117795 0.1081027,-0.178929 0.1327054 l -0.01938 0.0082 c -0.0045 0.00224,-0.0097 0.00298,-0.01417 0.00298 c -0.01491 0.0,-0.02833 -0.00895,-0.0343 -0.023112 c -0.0082 -0.018638,7.46E-4 -0.041005,0.02013 -0.04846 l 0.01938 -0.0082 c 0.05592 -0.023112,0.08425 -0.03504,0.150598 -0.1118304 c 0.01342 -0.016402,0.03728 -0.017893,0.05293 -0.00447 c 0.01566 0.01342,0.01715 0.036531,0.0037 0.052188 z'/%3E%3Cpath style='stroke-width:0.414187' fill='%23008000' id='path59' d='m 15.068654 1.2069086 c -0.01938 -0.030567,-0.04473 -0.046969,-0.07306 -0.055915 c -0.09916 0.1334509,-0.448067 0.3354911,-0.798469 0.3354911 c -0.09319 0.0,-0.187129 -0.014165,-0.275848 -0.047714 C 13.682706 1.3478158,13.519433 1.2210743,13.36138 1.0988064 C 13.23613 1.0011412,13.117589 0.90944034,12.972955 0.8468153 c -0.01938 -0.008946,-0.02758 -0.030567,-0.01938 -0.0492054 c 0.0082 -0.0193839,0.02982 -0.0275848,0.04921 -0.0193839 c 0.152835 0.0670982,0.275103 0.16103578,0.40408 0.26168305 c 0.153581 0.1192858,0.313125 0.2422992,0.540514 0.3287813 c 0.231861 0.087973,0.492054 0.030567,0.68962 -0.058152 c -0.08499 -0.2624286,-0.634451 -1.0608975,-1.540277 -0.85736626 c -0.14016 0.0313125,-0.262428 0.0603884,-0.369785 0.0857366 c -0.454777 0.10661162,-0.649362 0.15208936,-0.851402 0.0984107 c 0.01864 0.0939376,0.06784 0.17370984,0.146125 0.23782597 c 0.156562 0.12599558,0.383951 0.15730807,0.492053 0.15879917 c -0.05517 -0.062625,-0.114067 -0.10288399,-0.186384 -0.12823221 c -0.01938 -0.00671,-0.02982 -0.0275848,-0.02311 -0.0469688 c 0.0067 -0.0201295,0.02833 -0.0298214,0.04771 -0.0231116 c 0.234844 0.0827545,0.342947 0.2929955,0.517402 0.72167873 c 0.182656 0.4488126,0.821581 0.8670582,1.339728 0.7119868 c 0.527839 -0.1580536,0.518893 -0.7142233,0.518147 -0.7201876 c 0.0 -0.014911,0.0089 -0.029076,0.02237 -0.03504 c 0.03504 -0.015656,0.214715 -0.097665,0.341456 -0.2169509 c 0.0022 -0.00224,0.0045 -0.00373,0.0067 -0.00373 v -7.436E-4 c -0.006 -0.036531,-0.0164 -0.065607,-0.02908 -0.085737 M 13.832556 0.8132656 c 0.01417 -0.0156562,0.03728 -0.0164018,0.05293 -0.002982 c 0.103629 0.0939375,0.159544 0.1945848,0.161781 0.1990581 c 0.01044 0.017893,0.0037 0.040259,-0.01417 0.050696 c -0.006 0.00298,-0.01193 0.00447,-0.01789 0.00447 c -0.01342 0.0,-0.02609 -0.00745,-0.0328 -0.019384 c -7.45E-4 -7.456E-4,-0.05293 -0.0939375,-0.14687 -0.17967416 c -0.01566 -0.0134196,-0.0164 -0.0372768,-0.003 -0.0521875 M 13.204814 1.3209755 c -0.0015 0.0,-0.02311 0.00373,-0.06561 0.00373 c -0.03355 0.0,-0.07903 -0.00224,-0.137178 -0.00969 c -0.02088 -0.00224,-0.03504 -0.020875,-0.03206 -0.04175 c 0.0022 -0.02013,0.02088 -0.034295,0.04175 -0.032058 c 0.120777 0.015656,0.179675 0.00671,0.18042 0.00671 c 0.02013 -0.00373,0.03951 0.00969,0.0425 0.030567 c 0.0037 0.020129,-0.0097 0.038768,-0.02982 0.042496 m 0.90359 0.4495581 c -0.07754 0.091701,-0.117795 0.1081027,-0.178929 0.1327054 l -0.01938 0.0082 c -0.0045 0.00224,-0.0097 0.00298,-0.01417 0.00298 c -0.01491 0.0,-0.02833 -0.00895,-0.0343 -0.023112 c -0.0082 -0.018638,7.46E-4 -0.041005,0.02013 -0.04846 l 0.01938 -0.0082 c 0.05592 -0.023112,0.08425 -0.03504,0.150598 -0.1118304 c 0.01342 -0.016402,0.03728 -0.017893,0.05293 -0.00447 c 0.01566 0.01342,0.01715 0.036531,0.0037 0.052188 z'/%3E%3Cpath style='stroke-width:0.414187' id='path61' d='m 13.234636 1.2784799 c 0.0037 0.020129,-0.0097 0.038768,-0.02982 0.042496 c -0.0015 0.0,-0.02311 0.00373,-0.06561 0.00373 c -0.03355 0.0,-0.07903 -0.00224,-0.137178 -0.00969 c -0.02088 -0.00224,-0.03504 -0.020875,-0.03206 -0.04175 c 0.0022 -0.02013,0.02088 -0.034295,0.04175 -0.032058 c 0.120777 0.015656,0.179675 0.00671,0.18042 0.00671 c 0.02013 -0.00373,0.03951 0.00969,0.0425 0.030567 z'/%3E%3C/svg%3E";

  public static final String INPUT_COUNTRY = "country";
  public static final String COUNTRY_V_USA = "USA";
  public static final String COUNTRY_V_GE = "GE";
  public static final String COUNTRY_V_FR = "FR";
  public static final String COUNTRY_V_EN = "EN";
  public static final String COUNTRY_V_IT = "IT";
  public static final String COUNTRY_V_SP = "SP";
  public static final String OUTPUT_MESSAGE = "Message";
  public static final String INPUT_STATE = "state";
  public static final String BPMN_ERROR_NOTIME = "NOTIME";

  Logger logger = LoggerFactory.getLogger(HelloWordWorker.class.getName());

  public HelloWordWorker() {
    super( // Type
        "helloword",
        // List of Input
        Arrays.asList(RunnerParameter.getInstance(INPUT_COUNTRY, "Country name", String.class,
                    RunnerParameter.Level.OPTIONAL, "Country to whom to say hello")
                .addChoice(COUNTRY_V_USA, "United State of America")
                .addChoice(COUNTRY_V_GE, "Germany")
                .addChoice(COUNTRY_V_FR, "France")
                .addChoice(COUNTRY_V_EN, "England")
                .addChoice(COUNTRY_V_IT, "Italy")
                .addChoice(COUNTRY_V_SP, "Spain"),

            RunnerParameter.getInstance(INPUT_STATE, "State", String.class,
                    RunnerParameter.Level.OPTIONAL, "State")
                .addCondition(INPUT_COUNTRY, Collections.singletonList(COUNTRY_V_USA))),

        // list of Output
        Collections.singletonList(
            RunnerParameter.getInstance(OUTPUT_MESSAGE, OUTPUT_MESSAGE, String.class,
                RunnerParameter.Level.REQUIRED, "Welcome Message")),
        // List of BPMN Error
        Collections.singletonList(
            BpmnError.getInstance(BPMN_ERROR_NOTIME, "Sorry, no time to say hello now, I'm busy")));
  }

  @Override
  public String getName() {
    return "SayHello";
  }

  @Override
  public String getCollectionName() {
    return "example";
  }

  @Override
  public String getDescription() {
    return "Return a Say Hello message";
  }

  @Override
  public String getLogo() {
    return LOGO;
  }

  @Override
  public void execute(final JobClient jobClient,
                      final ActivatedJob activatedJob,
                      ContextExecution contextExecution) {

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    if (calendar.get(Calendar.HOUR_OF_DAY) < 6)
      throw new ConnectorException(BPMN_ERROR_NOTIME,
          "Sorry, too early in the morning, I'm not weak up");

    String country = getInputStringValue(INPUT_COUNTRY, null, activatedJob);
    String state = getInputStringValue(INPUT_STATE, null, activatedJob);
    String message =
        "Hello" + (country != null ? " " + country : "") + (state != null ? " " + state : "");

    setOutputValue(OUTPUT_MESSAGE, message, contextExecution);
  }
}
`````