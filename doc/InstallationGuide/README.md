# Installation guide
The purpose of this document is to explain how to install the Runtime.


# Introduction
the purpose of this document is to explain how to install and execute the runtime. The target audience are the administrators.

The runtime is available in multiple forms.

* A GitHub project (this project). The repository can produce the different artefact or can be run directly

* A Docker image. The docker image is accessible via the Docker repository or can be built from the project.

The docker image is used in the docker-compose, and the Kubernetes installation. 

The runtime accept new connectors / workers. They come from a JAR file.

The runtime accepts multiple way to upload them:
* Start the runtime. Access the Administration page, section `content`, and upload the JAR file. The Cherry runtime saved the JAR in a database, and it will be started if you stop / restart the runtime.

* Start the runtime. Access the Administration page, section `Store`. Upload the JAR file. It is saved in the local database, and wil be started if you stop / restart the runtime
 
* share (mount) a PVC with the runtime. Place in this PVC all connectors / worker JAR. At the beginning, the Cherry Runtime check the PVC, and any JAR or new version is uploaded in the database. This does not require any manual intervention
Check the section "Load connectors and workers at startup from a PVC"

* in the configuration, update the variable `ConnectorsAtStartup`. The MarketPlace is contacted, and connectors are downloaded. This does not require any manual intervention.
Visit the section "Load Connectors from MarketPlace"




# From a docker-compose

Visit [Docker Compose Installation](..%2F..%2Fdocker%2FREADME.md). This folder contains a H2 and a Postgres execution.

# Kubernetes Cluster

# Locally in a development environment

The runtime is available in multiple forms.

* A GitHub project (this project). The repository can produce the different artefact or can be run directly

* A Docker image. The docker image is accessible via the Docker repository or can be built from the project.

## Start from the project

Clone the project on your machine, or download it as a ZIP. When the project is on your machine, start it via:

````
mvn spring-boot:run
````
This command recompile the project, and start it.

To start it faster:
1. Recompile the project (only one time)
````
mvn install
````

2. Start it 
````
java -jar .\target\zeebe-cherry-runtime-<version>-exec.jar
````
Note: you need to replace the version by the correct value. Check the current value by listing the contents of the directory

## Create your docker image

Note: you may want to specify the Zeebe connection before starting. Check the next item.

You can create the Docker Image by this command
````
mvn install
docker build -t zeebe-cherryruntime:<Version> .
````

You can pull the image available in the Docker Repository
`````
docker pull zeebe-cherryruntime:<Version> .
`````


Run it with
`````
docker run -p 8888:9081 zeebe-cherryruntime:<Version> .
`````

In the directory `docker-cherry`, check different docker-compose example file.

Access the project [Zeebe-cherry-framework-helm](https://github.com/camunda-community-hub/zeebe-cherry-framework-helm) 
to access some Helm chart to deploy the runtime.



## Specify the connection to Zeebe
When you start the runtime, it will require a Zeebe Connection.

You can set up this Zeebe Connection:
* from the UI, in the section `parameters`. This information is stored in the local database and have to be provided only one time
* Before the execution, in the `src/main/resources/application.yaml`

`````yaml
# use a OnPremise Zeebe engine
zeebe.client:
  broker.gateway-address: 127.0.0.1:26500
  # zeebe.client.broker.gateway-address=host.docker.internal:26500
  security.plaintext: true

# use a cloud Zeebe engine
# zeebe.client.cloud.region=bru-2
# zeebe.client.cloud.clusterId=f867aa3d-5ee7-4324-96e8-21f557f104af
# zeebe.client.cloud.clientId=si71NBAWtUlREmfKja5oQC3M.WsT~sHa
# zeebe.client.cloud.clientSecret=pCTITndkvyNsaSkfJ-ji5w38vaP19QAXWURM3UVC1.J.eVyxa44uITFv3_c8iFi2

`````

* For the docker image, via some environment variable
 
`````yaml
  cherry-runtime:
    image: "zeebe-cherryruntime:3.0.0"
    container_name: "Cherry-runtime"
    ports:
      - "9081:9081"
    environment:
      - ZEEBE_CLIENT_BROKER_GATEWAY_ADDRESS=host.docker.internal:26500
      - ZEEBE_CLIENT_SECURITY_PLAINTEXT=true
      - ZEEBE_CLIENT_CLOUD_REGION=
      - ZEEBE_CLIENT_CLOUD_CLUSTERID=
      - ZEEBE_CLIENT_CLOUD_CLIENTID=
      - ZEEBE_CLIENT_CLOUD_CLIENTSECRET=   
`````
Doing this way, if you delete the container and restart it from scratch, you don't need to access again the parameter.



### Use a Camunda Saas Cloud

1. Follow the [Getting Started Guid](https://docs.camunda.io/docs/guides/getting-started/) to create an account, a
   cluster and client credentials

2. Use the client credentials to fill the following environment variables:
    * `ZEEBE_ADDRESS`: Address where your cluster can be reached.
    * `ZEEBE_CLIENT_ID` and `ZEEBE_CLIENT_SECRET`: Credentials to request a new access token.
    * `ZEEBE_AUTHORIZATION_SERVER_URL`: A new token can be requested at this address, using the credentials.

3. fulfill the information in the application.yaml, or via the Docker environment variable
````
# use a cloud Zeebe engine
zeebe.client.cloud.region=
zeebe.client.cloud.clusterId=
zeebe.client.cloud.clientId=
zeebe.client.cloud.clientSecret=
````

### Use a onPremise Zeebe

Use this information in the application.properties, and provide the IP address to the Zeebe engine

````
# use a onPremise Zeebe engine
zeebe.client.broker.gateway-address=127.0.0.1:26500
````

# Load connectors and workers at startup from a PVC

The Cherry runtime monitor a path. This path is part of the configuration

````yaml
cherry.connectorslib:
  uploadpath: ./localstorage/upload
  classloaderpath: ./localstorage/classloader
  forcerefresh: false
````

Each JAR file in the `uploadpath` is loaded in the runtime.
The second path, `classloaderpath`, is used internally to save all JAR files. A connector/worker can be uploaded directly
or can be downloaded from a *store*. To be loaded by the Java machine, it must be saved on the disk.
The last parameter, `forcerefresh`, is used to force the runtime to reload all JAR files on the upload path systematically. Else, the
runtime checks at the startup if the JAR is already loaded and ignores it if this is the case.

Doing this way, it is possible to start a complete runtime, with all the connectors and workers, in a completely automatic way.

TODO: describe an example



# Load Connectors from MarketPlace
TODO


# Database
The runtime use a database to save statistics, state (active/inactive) on connector / worker.
By default, a H2 database is used, for each runtime.

To save the information in a long term database, and create a farm of runtime, collecting in a single place all the information, a SQL database
can be connected.

This is possible by updating the variable with different values
````yaml
spring.datasource:
  url: "jdbc:h2:file:./cherry.db"
  driver-class-name: "org.h2.Driver"
  username: "sa"
  password: "password"
spring.jpa.database-platform: "org.hibernate.dialect.H2Dialect"
org.hibernate.dialect: "H2Dialect"
````

A JDBC Driver is required to access the database. By default, the runtime onboards two drivers: H2 and POSTGRESQL.
For any new driver, the `pom.xml` must be upgraded, and the application must be rebuilt.

Check the `docker-cherry/docker-compose-postgres.yml` example to see a different access.

