# Create my collection

## Create your project in the Camunda Community Hub 
This section is not mandatory. Maybe you want to keep your collection private, or it does contains only specific Workers or Connector?

Else, it is awesome if you like to share your work!

First, you must be able to create a new project. Go to https://github.com/camunda-community-hub and check https://github.com/camunda-community-hub#how-to-contribute

Follow the procedure, and you will be allowed to create a project.

For any Zeebe collection, the convition is zeebe-cherry-<Name of your collection>

For example,
`````
zeebe-cherry-officepdf
zeebe-cherry-cmis
`````


## Create your project
Create your project. it should be a Maven project. 

In your pom.xml, reference the library

`````
  <dependency>
      <groupId>org.camunda.community</groupId>
      <artifactId>zeebe-cherry-framework</artifactId>
      <version>${cherry.version}</version>
    </dependency>

`````
Check the last version number.

The project needs another dependencies:
````
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
      <version>${connector-core.version}</version>
    </dependency>
    <dependency>
      <groupId>io.camunda.connector</groupId>
      <artifactId>connector-validation</artifactId>
      <version>${connector-validation.version}</version>
    </dependency>

````

Access the project https://github.com/camunda-community-hub/zeebe-cherry-officepdf to have an example.




## Produce a Docker image 

Your project can produce a Docker image, register it in a repository, and then it can be accessible directly from docker pull, docker-compose or Helm

### Add the Docker file
First, a Docker Image must be present to produce the image.

Look at https://github.com/camunda-community-hub/zeebe-cherry-officepdf/blob/main/Dockerfile

Add any library you need in the image.

You can verify the image via the command

`````
docker build -t zeebe-chrry-mycollection .
`````


### Register in ... to let's GitHub to publish the Docker Image

### Add this Github Workflow

### Create an Helm Chart
