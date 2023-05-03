
# Developer guide

This section focus on the development part, to create a new collection of connector/worker.
The developer can choose two different pattern:
* connector
* worker

The connector pattern embed the Connector SDK
![Architecture](../Architecture.png?raw=true)

In the next part of the documentation, we use the term of **Runner**. A Runner is a Connector or a Worker.

## Step-by-step guide
Create your new Spring Boot Maven project.
Include in your pom.xml the library

````
<dependency>
    <groupId>org.camunda.community</groupId>
    <artifactId>zeebe-cherry-framework</artifactId>
    <version>${Cherry.version}</version>
</dependency>
````

Library is deployed in https://artifacts.camunda.com/ui/native/camunda-bpm-community-extensions/org/camunda/community/zeebe-cherry-framework

## Includes artefacts

Check the pom.xml, and add the different artefacts

* use spring-boot:run
  Add the spring-boot-maven-plugin dependency and the spring parent dependency

* start.sh/start.bat
  Copy this two files in your collection

## Setup the applications.properties

````
spring.application.name=ZeebeCherry
server.servlet.context-path=/
spring.mustache.prefix=classpath:static/templates/
````

## Embeded an existing connector
See the documentation relative to this pattern


## Develop your own runner
Create a new Java class for your runner. Extends the class AbstractWorker for a Worker, AbstractConnector for a Connector

In doing that, you have to respect some rules:

* Define the input and output of your runner.
  Which input does it need? Which output will it produce? This information is used to help the designer to understand how to use your runner
  and provide you comfort: if you declare that an input LONG is mandatory, then the framework checks for you its existence.
  When your code is called, you can be sure to have all you need.

* Define the different BPMN Errors that your worker can return


The abstract class offer some additional function, as a collection of getInput<TypedValue>() and setOutputValue() to access input and produce output data.
To normalize log messages, logInfo() and logError() methods are available.

Camunda 8 does not manipulate files. The library offers a mechanism to manipulate them via different implementations.
The designer can choose how he wants to save the file itself (in a JSON variable? On a shared disk? In a CMIS system, or in a database).
Your runner does not need to handle that complexity. Just declare you need a file as Input or Output, and the library does the work.

For your information, the framework comes with some generic runner. It should be started by itself as a server.

## Connector or Worker?

What is the difference between a ``Worker`` and a ``Connector``?
Actually, a ``Connector`` is used behind the scene for the ``Worker`` implementation.

The ``AbstractConnector`` implement the interface ``OutboundConnectorFunction``.
The main difference comes from parameters.

In a ``Worker``, you access a list of variables. The worker's signature gets an object to access anything.
The ``connector`` accepts one input, which is an object. This object may have multiple parameters.
Same as output: a ``worker`` produces a list of Output values when the ``connector`` provides one object.

From the Designer's point of view, there is no difference in the usage.

From the Cherry Framework, it will handle both element in the same ways: this is a ``Runner``


## Principle
Let's create a new runner in your project. In order to keep the project consistent, some view rules has to be followed.
The model is the collection message (``org.camunda.cherry.message``), and the worker ``SendMessageWorker``.

- The first level of the Cherry project is the collection name. Your connector must be attached in a collection, and may be under a sub collection.
  A collection is a package. For example, the runner ``SendMessage`` is under the collection ``message``.

- The worker must be suffixed by the name ``Worker``, a connector by the name ``Connector``.
  Example: Package is ``org.camunda.cherry.message``, class is ``SendMessageWorker``.
  If this class need another class, it can be saved under the same package, or under a sub package.

- type name Convention: type should start by ``v-<collectionName>-`` to identify a Cherry connector and don't mixup with different connectors.
  It must follow the snake case convention (https://en.wikipedia.org/wiki/Snake_case)
  Example: ``c-message-send``.

- README in each collection. This file explains all runners present in the collection. For each runner, this information has to be fulfilled:
    - the runner name
    - a description
    - the type
    - Inputs
    - Outputs
    - Errors the runner can throw.

## Tutorial - step by step

Follow this tutorial to start your first Cherry collection

### Maven
Create a project under Eclipse/Intellij, and use this skeleton to create your first project

````
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.camunda.community</groupId>
    <artifactId>zeebe-cherry-officepdf-workers</artifactId>
    <version>1.0.0</version>



    <properties>
    
        <!-- Update this version to the last information -->
        <zeebe.version>8.1.0</zeebe.version>
        <cherry.version>1.3.2</cherry.version>
        <connector.version>0.2.0</connector.version>
        <connectorvalidation.version>0.2.2</connectorvalidation.version>
        <junit.jupiter.version>5.9.0</junit.jupiter.version>

        <java.version>17</java.version>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <spring.boot.version>2.7.5</spring.boot.version>

    </properties>

    <!-- Inherit defaults from Spring Boot -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.5</version>
    </parent>

    <dependencies>

        <!-- Cherry framework -->    
        <dependency>
            <groupId>org.camunda.community</groupId>
            <artifactId>zeebe-cherry-framework</artifactId>
            <version>${cherry.version}</version>
        </dependency>


        <!-- Zeebe Client -->
        <dependency>
            <groupId>io.camunda</groupId>
            <artifactId>spring-zeebe-starter</artifactId>
            <version>${zeebe.version}</version>
        </dependency>

        <dependency>
            <groupId>io.camunda</groupId>
            <artifactId>zeebe-client-java</artifactId>
            <version>${zeebe.version}</version>
        </dependency>

        <!-- Accept Camunda Connector -->
        <dependency>
            <groupId>io.camunda.connector</groupId>
            <artifactId>connector-core</artifactId>
            <version>${connector.version}</version>
        </dependency>
        <dependency>
            <groupId>io.camunda.connector</groupId>
            <artifactId>connector-runtime-job-worker</artifactId>
            <version>${connector.version}</version>
        </dependency>
        <dependency>
            <groupId>io.camunda.connector</groupId>
            <artifactId>connector-validation</artifactId>
            <version>${connectorvalidation.version}</version>
        </dependency>

        <!-- Manipulate variables -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.9.0</version>
        </dependency>

        <!-- JSON LocalDateTime -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>2.13.3</version>
        </dependency>

        <!-- tests -->
        <dependency>
            <groupId>io.zeebe</groupId>
            <artifactId>zeebe-worker-java-testutils</artifactId>
            <version>8.0.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>${junit.jupiter.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>${junit.jupiter.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>


    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.21.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.platform</groupId>
                        <artifactId>junit-platform-surefire-provider</artifactId>
                        <version>1.2.0</version>
                    </dependency>
                </dependencies>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
            <!-- allow mvn run -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>CherryApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>



</project>

````

### Application.properties
Set up in the application.properties the connection the Zeebe server

````
# use a OnPremise Zeebe engine
zeebe.client.broker.gateway-address=127.0.0.1:26500
zeebe.client.security.plaintext=true
````

and setup the Mustache Spring framework

````
# Spring Boot Configuration
spring.application.name=ZeebeCherry
server.servlet.context-path=/
spring.mustache.prefix=classpath:static/templates/
````

### Copy start.sh / start.bat
Copy these two files from the cherry framework

### Start the framework
Start the framework by using the start.sh/start.bat, or run
````
mvn spring-boot:run
````

Access the page `localhost:8080`



### Your first connector/worker
Create a Java Class.
Check the example `src/main/java/org/camunda/cherry/ping/PingWorker.java`

**Declare the class**
In your Java Class, add the @Component, and extends the AbstractWorker or the AbstractConnector
````
@Component
public class PingWorker extends AbstractWorker {
````

**Constructor**
Declare in the constructor all the mandatory parameters
````
public PingWorker() {
    super("c-ping",
        Arrays.asList(
            RunnerParameter.getInstance("message", "Message to log", String.class, RunnerParameter.Level.OPTIONAL, "Message to log, to ensure the worker was called"),
            RunnerParameter.getInstance("delay", "Delay in ms", Long.class, RunnerParameter.Level.OPTIONAL, "Delay to sleep, in milliseconds")
        ),
        Arrays.asList(
            RunnerParameter.getInstance(OUTPUT_TIMESTAMP, "Time stamp", String.class, RunnerParameter.Level.REQUIRED, "Produce a timestamp")
        ),
        Collections.emptyList()
    );
 }
````

Let's review parameters per parameters
**"c-ping"**
The first parameter is the type. This is referenced in the service task, and the worker will catch all jobs related to this type

**List of Inputs**
````
Arrays.asList(
RunnerParameter.getInstance("message", "Message to log", String.class, RunnerParameter.Level.OPTIONAL, "Message to log, to ensure the worker was called"),
RunnerParameter.getInstance("delay", "Delay in ms", Long.class, RunnerParameter.Level.OPTIONAL, "Delay to sleep, in milliseconds")
),
````
Give a list of Input that your worker expect.
Each parameter has:
- a name (message)
- a label (Message to log). This label is visible in the Element Template for example
- a type. Multiple getter are available to access variables
- a scope: OPTIONAL, MANDATORY
- a description

**List of outputs**
Same as input, a list of outputs is required. This list will explain what your connector returns.

````
     Arrays.asList(RunnerParameter.getInstance("timestamp", "Time stamp", String.class, RunnerParameter.Level.REQUIRED, "Produce a timestamp")
````


**List of BPMN Errors**
Give the list of BPMN Error that your provide. This list will be available in the documentation.


**Optional method**
You can override different method:
````
     @Override
     public String getName() {
        return "MyConnector";
    }

    @Override
    public String getLabel() {
        return "My First Connector";
    }

    @Override
    public String getDescription() {
        return "Verify that the connector is visible in the Modeler via the template, and is executed correctly.";
    }

  @Override
    public String getLogo() {
        return WORKERLOGO;
    }

    private static final String WORKERLOGO = "data:image/svg+xml,%3C?xml version='1.0' encoding='UTF-8' standalone='no'?%3E%3Csvg   xmlns:dc='http://purl.org/dc/elements/1.1/'   xmlns:cc='http://creativecommons.org/ns%23'   xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns%23'   xmlns:svg='http://www.w3.org/2000/svg'   xmlns='http://www.w3.org/2000/svg'   version='1.1'   id='Capa_1'   x='0px'   y='0px'   viewBox='0 0 18 18'   xml:space='preserve'   width='18'   height='18'%3E%3Cmetadata   id='metadata55'%3E%3Crdf:RDF%3E%3Ccc:Work       rdf:about=''%3E%3Cdc:format%3Eimage/svg+xml%3C/dc:format%3E%3Cdc:type         rdf:resource='http://purl.org/dc/dcmitype/StillImage' /%3E%3Cdc:title%3E%3C/dc:title%3E%3C/cc:Work%3E%3C/rdf:RDF%3E%3C/metadata%3E%3Cdefs   id='defs53' /%3E%3Cg   id='g18'   transform='scale(0.3)'%3E %3Cpath   d='M 30,0 C 13.458,0 0,13.458 0,30 0,46.542 13.458,60 30,60 46.542,60 60,46.542 60,30 60,13.458 46.542,0 30,0 Z m 0,58 C 14.561,58 2,45.439 2,30 2,14.561 14.561,2 30,2 45.439,2 58,14.561 58,30 58,45.439 45.439,58 30,58 Z'   id='path2' /%3E %3Cpath   d='M 23.165,8.459 C 23.702,8.329 24.033,7.789 23.904,7.253 23.775,6.716 23.234,6.387 22.698,6.514 18.763,7.46 15.176,9.469 12.322,12.323 9.468,15.177 7.46,18.764 6.514,22.698 c -0.129,0.536 0.202,1.076 0.739,1.206 0.078,0.019 0.157,0.027 0.234,0.027 0.451,0 0.861,-0.308 0.972,-0.767 0.859,-3.575 2.685,-6.836 5.277,-9.429 2.592,-2.593 5.854,-4.417 9.429,-5.276 z'   id='path4' /%3E %3Cpath   d='m 52.747,36.096 c -0.538,-0.129 -1.077,0.201 -1.206,0.739 -0.859,3.575 -2.685,6.836 -5.277,9.429 -2.592,2.593 -5.854,4.418 -9.43,5.277 -0.537,0.13 -0.868,0.67 -0.739,1.206 0.11,0.459 0.521,0.767 0.972,0.767 0.077,0 0.156,-0.009 0.234,-0.027 3.936,-0.946 7.523,-2.955 10.377,-5.809 2.854,-2.854 4.862,-6.441 5.809,-10.376 0.128,-0.536 -0.203,-1.076 -0.74,-1.206 z'   id='path6' /%3E %3Cpath   d='m 24.452,13.286 c 0.538,-0.125 0.873,-0.663 0.747,-1.2 -0.125,-0.538 -0.665,-0.878 -1.2,-0.747 -3.09,0.72 -5.904,2.282 -8.141,4.52 -2.237,2.236 -3.8,5.051 -4.52,8.141 -0.126,0.537 0.209,1.075 0.747,1.2 0.076,0.019 0.152,0.026 0.228,0.026 0.454,0 0.865,-0.312 0.973,-0.773 0.635,-2.725 2.014,-5.207 3.986,-7.18 1.972,-1.973 4.456,-3.352 7.18,-3.987 z'   id='path8' /%3E %3Cpath   d='m 48.661,36.001 c 0.126,-0.537 -0.209,-1.075 -0.747,-1.2 -0.538,-0.133 -1.075,0.209 -1.2,0.747 -0.635,2.725 -2.014,5.207 -3.986,7.18 -1.972,1.973 -4.455,3.352 -7.18,3.986 -0.538,0.125 -0.873,0.663 -0.747,1.2 0.107,0.462 0.519,0.773 0.973,0.773 0.075,0 0.151,-0.008 0.228,-0.026 3.09,-0.72 5.904,-2.282 8.141,-4.52 2.236,-2.236 3.798,-5.05 4.518,-8.14 z'   id='path10' /%3E %3Cpath   d='m 26.495,16.925 c -0.119,-0.541 -0.653,-0.879 -1.19,-0.763 -4.557,0.997 -8.146,4.586 -9.143,9.143 -0.118,0.539 0.224,1.072 0.763,1.19 0.072,0.016 0.144,0.023 0.215,0.023 0.46,0 0.873,-0.318 0.976,-0.786 0.831,-3.796 3.821,-6.786 7.617,-7.617 0.538,-0.118 0.88,-0.651 0.762,-1.19 z'   id='path12' /%3E %3Cpath   d='m 43.838,34.695 c 0.118,-0.539 -0.224,-1.072 -0.763,-1.19 -0.54,-0.118 -1.072,0.222 -1.19,0.763 -0.831,3.796 -3.821,6.786 -7.617,7.617 -0.539,0.118 -0.881,0.651 -0.763,1.19 0.103,0.468 0.516,0.786 0.976,0.786 0.071,0 0.143,-0.008 0.215,-0.023 4.556,-0.997 8.145,-4.586 9.142,-9.143 z'   id='path14' /%3E %3Cpath   d='m 38.08,30 c 0,-4.455 -3.625,-8.08 -8.08,-8.08 -4.455,0 -8.08,3.625 -8.08,8.08 0,4.455 3.625,8.08 8.08,8.08 4.455,0 8.08,-3.625 8.08,-8.08 z M 30,36.08 c -3.353,0 -6.08,-2.728 -6.08,-6.08 0,-3.352 2.728,-6.08 6.08,-6.08 3.352,0 6.08,2.728 6.08,6.08 0,3.352 -2.727,6.08 -6.08,6.08 z'   id='path16' /%3E%3C/g%3E%3Cg   id='g20'%3E%3C/g%3E%3Cg   id='g22'%3E%3C/g%3E%3Cg   id='g24'%3E%3C/g%3E%3Cg   id='g26'%3E%3C/g%3E%3Cg   id='g28'%3E%3C/g%3E%3Cg   id='g30'%3E%3C/g%3E%3Cg   id='g32'%3E%3C/g%3E%3Cg   id='g34'%3E%3C/g%3E%3Cg   id='g36'%3E%3C/g%3E%3Cg   id='g38'%3E%3C/g%3E%3Cg   id='g40'%3E%3C/g%3E%3Cg   id='g42'%3E%3C/g%3E%3Cg   id='g44'%3E%3C/g%3E%3Cg   id='g46'%3E%3C/g%3E%3Cg   id='g48'%3E%3C/g%3E%3Cg   id='g85'   transform='matrix(1.1259408,0,0,1.1259408,33.760593,8.6647721)'%3E%3Cpath     d='m -15.969199,-1.3973349 c -0.04428,0.56592004 -0.49572,0.83754004 -0.58752,0.88722004 -0.4887,0.26676 -1.31112,0.17388 -1.70856,-0.38556 -0.21924,-0.30780004 -0.3294,-0.79758004 -0.1161,-1.13670004 l 0.0027,-0.0054 c 0.07668,-0.12474 0.25434,-0.33534 0.58212,-0.34236 h 0.01188 c 0.11718,0 0.25218,0.03564 0.37152,0.06696 0.0837,0.02214 0.15606,0.04104 0.21006,0.04482 0.03402,0.00216 0.08262,-0.00648 0.14418,-0.01728 0.18414,-0.0324 0.4914,-0.0864 0.76032,0.10368 0.36612,0.2597403 0.33156,0.76356 0.3294,0.78462 z'     id='path57'     style='fill:%23aa0000;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -18.207499,-2.3007546 c -0.1053,0.07452 -0.17496,0.16686 -0.21708,0.2349 l -0.0027,0.00432 c -0.10908,0.17388 -0.13932,0.37584 -0.1188,0.57348 -0.0022,0.00108 -0.0043,0.00216 -0.0059,0.00378 -0.40014,0.3267 -0.85212,0.24246 -1.02762,0.19116 l -0.01026,-0.0027 c -0.5589,-0.13932 -1.11078,-0.74304 -1.00926,-1.38618 0.05616,-0.35424 0.324,-0.7614 0.72792,-0.85428 0.06966,-0.0162 0.43362,-0.0837 0.7263,0.16146 0.08964,0.0756 0.15606,0.1857601 0.21438,0.28296 0.04158,0.0702 0.07776,0.13014 0.11718,0.16956 0.02538,0.02538 0.06966,0.05022 0.12582,0.08208 0.1647,0.09234 0.4131,0.2322001 0.4779,0.53352 5.4e-4,0.00216 0.0011,0.00432 0.0022,0.00594 z'     id='path59'     style='fill:%23aa0000;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -17.110219,-2.3309949 c -0.03186,0.0054 -0.05994,0.00918 -0.07992,0.00918 -0.0032,0 -0.0065,-5.4e-4 -0.0092,-5.4e-4 -0.01512,-0.00108 -0.03294,-0.00324 -0.05184,-0.00702 0.16578,-0.42336 0.23436,-1.10052 -0.34182,-1.95804 -0.0011,-0.00216 -0.0027,-0.00378 -0.0049,-0.0054 0.03348,-0.13284 0.04482,-0.22572 0.04752,-0.2484 0.0016,-0.01512 -0.0092,-0.02808 -0.02376,-0.0297 -0.01512,-0.00216 -0.02808,0.00918 -0.0297,0.02376 -0.0065,0.05778 -0.07128,0.5788801 -0.4347,1.0416602 -0.22734,0.28944 -0.58536,0.5049 -0.70902,0.56646 -0.01134,-0.00756 -0.02106,-0.01512 -0.02754,-0.0216 -0.01782,-0.01782 -0.0351,-0.04104 -0.05292,-0.06858 0.01026,-0.0054 0.02106,-0.01134 0.0324,-0.01728 0.12906,-0.06912 0.34506,-0.1841401 0.60858,-0.4600801 0.31158,-0.32616 0.37098,-0.82566 0.2835,-0.96174 -0.04212,-0.06588 -0.10962,-0.07236 -0.16362,-0.07722 -0.07074,-0.00594 -0.1134,-0.01026 -0.12096,-0.10638 -0.007,-0.0837 0.0324,-0.15768 0.1107,-0.20736 0.1053,-0.0675 0.29376,-0.0891 0.47628,0.01458 0.11394,0.06426 0.11286,0.17388 0.11232,0.27972 -5.4e-4,0.07074 -0.0011,0.1377 0.03402,0.18576 l 0.01512,0.02052 c 0.0864,0.1171801 0.28836,0.3904201 0.40932,0.86724 0.12906,0.5102999 0.007,0.9752399 -0.07992,1.1604599 z'     id='path61'     style='fill:%23502d16;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -18.698899,-4.5455348 c -0.0043,0.00216 -0.0086,0.00324 -0.01296,0.00324 -0.0097,0 -0.0189,-0.0054 -0.02376,-0.01404 -5.4e-4,-5.4e-4 -0.03834,-0.06804 -0.10638,-0.13014 -0.01134,-0.00972 -0.01188,-0.027 -0.0022,-0.0378 0.01026,-0.01134 0.027,-0.01188 0.03834,-0.00216 0.07506,0.06804 0.11556,0.14094 0.11718,0.14418 0.0076,0.01296 0.0027,0.02916 -0.01026,0.03672 z'     id='path63'     style='stroke-width:0.3' /%3E%3Cpath     d='m -18.644359,-4.0309149 c -0.05616,0.06642 -0.08532,0.0783 -0.1296,0.09612 l -0.01404,0.00594 c -0.0032,0.00162 -0.007,0.00216 -0.01026,0.00216 -0.0108,0 -0.02052,-0.00648 -0.02484,-0.01674 -0.0059,-0.0135 5.4e-4,-0.0297 0.01458,-0.0351 l 0.01404,-0.00594 c 0.0405,-0.01674 0.06102,-0.02538 0.10908,-0.081 0.0097,-0.01188 0.027,-0.01296 0.03834,-0.00324 0.01134,0.00972 0.01242,0.02646 0.0027,0.0378 z'     id='path65'     style='stroke-width:0.3' /%3E%3Cpath     d='m -17.948839,-4.4391548 c -0.01404,-0.02214 -0.0324,-0.03402 -0.05292,-0.0405 -0.07182,0.09666 -0.32454,0.243 -0.57834,0.243 -0.0675,0 -0.13554,-0.01026 -0.1998,-0.03456 -0.1728,-0.06588 -0.29106,-0.15768 -0.40554,-0.24624 -0.09072,-0.07074 -0.17658,-0.13716 -0.28134,-0.18252 -0.01404,-0.00648 -0.01998,-0.02214 -0.01404,-0.03564 0.0059,-0.01404 0.0216,-0.01998 0.03564,-0.01404 0.1107,0.0486 0.19926,0.11664 0.29268,0.18954 0.11124,0.0864 0.2268,0.1755 0.3915,0.23814 0.16794,0.06372 0.3564,0.02214 0.4995,-0.04212 -0.06156,-0.19008 -0.45954,-0.76842 -1.11564,-0.6210001 -0.10152,0.02268 -0.19008,0.04374 -0.26784,0.0621 -0.3294,0.07722 -0.47034,0.11016 -0.61668,0.07128 0.0135,0.06804 0.04914,0.12582 0.10584,0.17226 0.1134,0.09126 0.2781,0.11394 0.3564,0.11502 -0.03996,-0.04536 -0.08262,-0.07452 -0.135,-0.09288 -0.01404,-0.00486 -0.0216,-0.01998 -0.01674,-0.03402 0.0049,-0.01458 0.02052,-0.0216 0.03456,-0.01674 0.1701,0.05994 0.2484,0.21222 0.37476,0.5227201 0.1323,0.32508 0.59508,0.62802 0.97038,0.5157 0.38232,-0.11448 0.37584,-0.51732 0.3753,-0.52164 0,-0.0108 0.0065,-0.02106 0.0162,-0.02538 0.02538,-0.01134 0.15552,-0.07074 0.24732,-0.15714 0.0016,-0.00162 0.0032,-0.0027 0.0049,-0.0027 v -5.4e-4 c -0.0043,-0.02646 -0.01188,-0.04752 -0.02106,-0.0621 m -0.89532,-0.2851201 c 0.01026,-0.01134 0.027,-0.01188 0.03834,-0.00216 0.07506,0.06804 0.11556,0.14094 0.11718,0.14418 0.0076,0.01296 0.0027,0.02916 -0.01026,0.03672 -0.0043,0.00216 -0.0086,0.00324 -0.01296,0.00324 -0.0097,0 -0.0189,-0.0054 -0.02376,-0.01404 -5.4e-4,-5.4e-4 -0.03834,-0.06804 -0.10638,-0.13014 -0.01134,-0.00972 -0.01188,-0.027 -0.0022,-0.0378 m -0.45468,0.36774 c -0.0011,0 -0.01674,0.0027 -0.04752,0.0027 -0.0243,0 -0.05724,-0.00162 -0.09936,-0.00702 -0.01512,-0.00162 -0.02538,-0.01512 -0.02322,-0.03024 0.0016,-0.01458 0.01512,-0.02484 0.03024,-0.02322 0.08748,0.01134 0.13014,0.00486 0.13068,0.00486 0.01458,-0.0027 0.02862,0.00702 0.03078,0.02214 0.0027,0.01458 -0.007,0.02808 -0.0216,0.03078 m 0.65448,0.32562 c -0.05616,0.06642 -0.08532,0.0783 -0.1296,0.09612 l -0.01404,0.00594 c -0.0032,0.00162 -0.007,0.00216 -0.01026,0.00216 -0.0108,0 -0.02052,-0.00648 -0.02484,-0.01674 -0.0059,-0.0135 5.4e-4,-0.0297 0.01458,-0.0351 l 0.01404,-0.00594 c 0.0405,-0.01674 0.06102,-0.02538 0.10908,-0.081 0.0097,-0.01188 0.027,-0.01296 0.03834,-0.00324 0.01134,0.00972 0.01242,0.02646 0.0027,0.0378 z'     id='path67'     style='fill:%23008000;fill-opacity:1;stroke-width:0.3' /%3E%3Cpath     d='m -19.277239,-4.3873149 c 0.0027,0.01458 -0.007,0.02808 -0.0216,0.03078 -0.0011,0 -0.01674,0.0027 -0.04752,0.0027 -0.0243,0 -0.05724,-0.00162 -0.09936,-0.00702 -0.01512,-0.00162 -0.02538,-0.01512 -0.02322,-0.03024 0.0016,-0.01458 0.01512,-0.02484 0.03024,-0.02322 0.08748,0.01134 0.13014,0.00486 0.13068,0.00486 0.01458,-0.0027 0.02862,0.00702 0.03078,0.02214 z'     id='path69'     style='stroke-width:0.3' /%3E%3C/g%3E%3C/svg%3E";

    @Override
    public String getLogo() {
        return WORKERLOGO;
    }
    
````
The logo must be a SVG image.

**Execute**
Then here you are: the execute method!

````
@Override
public void execute(final JobClient jobClient, final ActivatedJob activatedJob, ContextExecution contextExecution) {
   String message = getInputStringValue("message", null, activatedJob);
   Long delay = getInputLongValue("delay", null, activatedJob);
   ...
   
   setOutputValue("timestamp", formattedDate, contextExecution);
}
````

You have different method to access the input, in the correct class.
To produce the result, use a setOutput method. This method verify that you respect the Output contract.

**File management**
C8 does not propose any solution to manipulate file.
Cherry propose a solution, to store files in different location, and to access it.
The storage can be
* in the temporary folder (which is nice, except if you are working in a Kubernetes cluster with different server/pod),
* in a specific folder. Then you can mount this location over different server/pod
* as a process variable, encoding the variable. But Zeebe limit the variable size to 4 Mb
* in a CMIS repository.

When you want to save a file for the first time, you need to specify the location. This is the *Storage definition*.
Cherry use this storage definition to do the job. A process variable is then calculated to access the file.

````
setOutputFileVariableValue("MyFile", storageDefinition, fileVariable, contextExecution);
````

To access the file, just use the method

````
FileVariable fileVariable = getInputFileVariableValue("MyFile", activatedJob);
````
Cherry will do the job to load the file where it is.

**Access the Element Template**

Restart the Cherry framework. Your connector appears in the dashboard, and the Element Template file can be download.

You can download the complete collection, to save for the desktop Modeler

Or you can access the definition worker per worker, to create a connector template in the Web Modeler. One connector template must be created one by obne.

## Tests
Java class test follows the same architecture, ``test/java/org.camunda.cherry.<CollectionName>``.

Process test should be saved under ``test/resources/<collectionName>``. For example, the `SendMessage.bpmn` test process is saved under `main/resources/message`



## Contract

Implementation works with the Contract concept.

A runner implementation is based on a skeleton, the abstract class ``AbstractWorker``. A typical implementation immediately call the parent class for each ``handleWorkerExecution``:
````
@ZeebeWorker(type = "v-pdf-convert-to", autoComplete = true)
public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
super.handleWorkerExecution(jobClient, activatedJob);
}
````
A Runner defines a set of expected variables, the INPUT. For each input, a level (OPTIONAL,REQUIRED), and a type (String? Double?) are provided.

The abstract class checks the requirement. If the contract is not respected, then a BPMN error is thrown.

So, when the method execution is called, implementation is sure that all required information is provided

````
public void execute(final JobClient jobClient, final ActivatedJob activatedJob) {
````

On the opposite, the Runner declares the list of Output variables it will be created. The abstract class checks that all output variables is correctly produced by the Runner, no more, no less. Suppose the output contract is not respected (you forgot one variable, or you provided an undeclared variable), a BPMN error is thrown.

A contract is very useful:
* As a developer, you don't need to worry about the existence of the variable. If you ask it, you will have it during the execution.
* As a designer, all Input and Output variables for a Runner are declared and documented.

This implied the implementation declare Inputs and Outputs
````
public OfficeToPdfWorker() {
  super("v-pdf-convert-to",
  Arrays.asList(
    AbstractWorker.WorkerParameter.getInstance(INPUT_SOURCE_FILE, Object.class, Level.REQUIRED, "FileVariable for the file to convert"),
    AbstractWorker.WorkerParameter.getInstance(INPUT_SOURCE_STORAGEDEFINITION, String.class, Level.REQUIRED, "Storage Definition use to access the file"),
    AbstractWorker.WorkerParameter.getInstance(INPUT_DESTINATION_FILE_NAME, String.class, Level.REQUIRED, "Destination file name"),
    AbstractWorker.WorkerParameter.getInstance(INPUT_DESTINATION_STORAGEDEFINITION, String.class, Level.REQUIRED, "Storage Definition use to describe how to save the file")
  ),
  Arrays.asList(
    AbstractWorker.WorkerParameter.getInstance(OUTPUT_DESTINATION_FILE, Object.class, Level.REQUIRED, "FileVariable converted")
  ),
  Arrays.asList(BPMERROR_CONVERSION_ERROR, BPMERROR_LOAD_FILE_ERROR));
}
````

To simplify the implementation, a set of getter() is provided to access any input.
````
public String getInputStringValue(String parameterName, String defaultValue, final ActivatedJob activatedJob) {
public Double getInputDoubleValue(String parameterName, Double defaultValue, final ActivatedJob activatedJob) {
````

and a **setValue()** is provided too.
This method must be used to set any output: the contract verification track the information you produce here.

## Manipulating files

Zeebe does not provide any mechanism to manipulate files.

Cherry project offers a mechanism, the storage definition.
Two methods are available:
````
public FileVariable getFileVariableValue(String parameterName, String storageDefinition, final ActivatedJob activatedJob) {

public void setFileVariableValue(String parameterName, String storageDefinition, FileVariable fileVariableValue) {
````
These methods exploit the storageDefinition and save or retrieve the file for the Runner.





