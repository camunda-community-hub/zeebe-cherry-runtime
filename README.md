[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

# Vercors project

The Vercors project is a collection of Workers ready to use for Zeebe.
Give the information to connect to your Zeebe engine in src/main/resources/application.properties.
You can give a local Zebee or a Camunda Clound Zeebe.

You can start the Vercors application on a server. This is a SpringBoot application, located here src/main/java/org/camunda/vercors/message/SendMessageWorker.java

This application starts with all workers. There is one thread for the application, which can be changed in the application.properties file (parameter zeebe.client.worker.threads)


# How to Use

Start the Vercors application.
The application connects to the Zeebe server and starts to monitor and execute all workers.

Check the different workers. Workers are grouped in a collection. All collections are visible under src/main/java/org/camunda/vercors.
Each folder is a collection, except for the definition folder.

Reach out to the message folder. A README.md is present here to explain the different workers available in the collection. Each worker declares:
a type. This is the marker for Zeebe to call the worker. Each task reference a type
* a list of Input variables, variables need to accomplish the work
* a list of the Output variable, variables produced by the worker

For example, in your process, to send a message, use the type "v-send-message", set up the different input (messageName as minimum), and that's it.

## Connection Setup
The connection to the Zeebe engine is piloted via the application.properties located on src/main/java/resources/application.properties

To connect to a set up a self-manage engine, used the parameter
zeebe.client.broker.gateway-address=127.0.0.1:26500
Give the correct IP + port number.

Note:
For a local installation (without authentication) you only need to set `ZEEBE_ADDRESS`

### Connect to Cluster Camunda Cloud
The connection to the Zeebe engine is piloted via the application.properties located on src/main/java/resources/application.properties


1. Follow the [Getting Started Guid](https://docs.camunda.io/docs/guides/getting-started/) to create an account, a
   cluster and client credentials
2. Use the client credentials to fill the following environment variables:
    * `ZEEBE_ADDRESS`: Address where your cluster can be reached.
    * `ZEEBE_CLIENT_ID` and `ZEEBE_CLIENT_SECRET`: Credentials to request a new access token.
    * `ZEEBE_AUTHORIZATION_SERVER_URL`: A new token can be requested at this address, using the credentials.
3. Run the application

## Example
See different examples under src/test/resources/org.camunda.vercors. You have a folder per collection, and processes in the collection.

# How to add a connector

## Principle
Let's create a new connector in the Vercors project. In order to keep the project consistent, some view rules has to be followed.
The model is the collection message (org.camunida.vercors.message), and the worder SendMessageWorker.

- The first level of the Vercors project is the collection name. Your connector must be attached in a collection, and may be under a sub collection.
A collection is a package. For example, the worker "SendMessage" is under the collection "message".

- The worker must be suffixed by the name "Worker".
Example: Package is org.camunda.vercors.message, class is SendMessageWorker.
If this class need another class, it can be saved under the same package, or under a sub package.

- type name Convention: type should start by v- to identify a Vercors connector and don't mixup with different connectors. 
It must follow the snake case convention (https://en.wikipedia.org/wiki/Snake_case) 
Example: v-send-message.

- README in each collection. This file explains all workers present in the collection. For each worker, these information has to be fulfill:
  - the worker name
  - a description
  - the type
  - the input
  - the output
  - the errors the connector can throws

## tests
Java class test follows the same architecture, test/java/org.camunda.vercors.<CollectionName>.

Process test should be saved under test/resources/<collectionName>. For example, the SendMessage.bpmn test process is saved under main/resources/message



## Contract
<Principe of a contract

## Implementation
<< create a new class
## Documentation

