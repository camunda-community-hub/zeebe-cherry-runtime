[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

# Vercors project

<<What is it?>>

# How to Use

The main method is in `Worker.java`. It requires a couple of environment variables to run.

## Connection Setup
<<how to connect to your Zeebe engine? Camunda cloud or on promise Zeebe engine

### Connect to Cluster Camunda Cloud

1. Follow the [Getting Started Guid](https://docs.camunda.io/docs/guides/getting-started/) to create an account, a
   cluster and client credentials
2. Use the client credentials to fill the following environment variables:
    * `ZEEBE_ADDRESS`: Address where your cluster can be reached.
    * `ZEEBE_CLIENT_ID` and `ZEEBE_CLIENT_SECRET`: Credentials to request a new access token.
    * `ZEEBE_AUTHORIZATION_SERVER_URL`: A new token can be requested at this address, using the credentials.
3. Run `Worker`

### Connect to local Installation

For a local installation (without authentication) you only need to set `ZEEBE_ADDRESS`

## Example

Either you deploy `process.bpmn` or you design your own process with a service task with the `greet` job type.

# How to add a connector

## Principle
Let's create a new connector in the Vercors project. In order to keep the project consistent, some view rule has to be followed.
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

