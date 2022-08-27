[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

# Zeebe Cherry Framework

This project is a framework to build workers for Zeebe.

The framework offers a library for your project and a set of administrative pages.

# Developer guide

## Step-by-step guide
Create your new Spring Boot Maven project.
Include in your pom.xml the library

<dependency>
    <groupId>org.camunda.community</groupId>
    <artifactId>zeebe-cherry-framework</artifactId>
    <version>1.0.0</version>
</dependency>

## Develop your own worker
Create a new Java class for your worker. Extends the class AbstractWorker.
In doing that, you have to respect some rules:
Define the input and output of your worker. Which input does it need? Which output will it produce? This information is used to help the designer to understand how to use your worker and provide you comfort: if you declare that an input LONG is mandatory, then the framework checks for you its existence. When your code is called, you can be sure to have all you need.
define the different BPMN Errors that your worker can return
The abstract class offer some additional function, as a collection of getInput<TypedValue>() and setValue() to access input and produce output data.
To normalize log messages, logInfo() and logError() methods are available.

Camunda 8 does not manipulate files. The library offers a mechanism to manipulate them via different implementations. The designer can choose how he wants to save the file itself (in a JSON variable? On a shared disk? In a CMIS system, or in a database). Your worker does not need to handle that complexity. Just declare you need a file as Input or Output, and the library does the work.

For your information, the framework comes with some generic workers. It should be started by itself as a server.



## Principle
Let's create a new worker in your project. In order to keep the project consistent, some view rules has to be followed.
The model is the collection message (**"org.camunida.cherry.message"**), and the worker **"SendMessageWorker"**.

- The first level of the Cherry project is the collection name. Your connector must be attached in a collection, and may be under a sub collection.
  A collection is a package. For example, the worker **"SendMessage"** is under the collection **"message"**.

- The worker must be suffixed by the name **"Worker"**.
  Example: Package is **"org.camunda.cherry.message"**, class is **"SendMessageWorker"**.
  If this class need another class, it can be saved under the same package, or under a sub package.

- type name Convention: type should start by **v-<collectionName>-** to identify a Cherry connector and don't mixup with different connectors.
  It must follow the snake case convention (https://en.wikipedia.org/wiki/Snake_case)
  Example: **"c-message-send**.

- README in each collection. This file explains all workers present in the collection. For each worker, this information has to be fulfilled:
    - the worker name
    - a description
    - the type
    - Inputs
    - Outputs
    - Errors the connector can throw.


## Tests
Java class test follows the same architecture, `test/java/org.camunda.cherry.<CollectionName>`.

Process test should be saved under test/resources/<collectionName>. For example, the `SendMessage.bpmn` test process is saved under `main/resources/message`



## Contract

Implementation works with the Contract concept.

A worker implementation is based on a skeleton, the abstract class **AbstractWorker**. A typical implementation immediately call the parent class for each handleWorkerExecution:
````
@ZeebeWorker(type = "v-pdf-convert-to", autoComplete = true)
public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
super.handleWorkerExecution(jobClient, activatedJob);
}
````
A worker defines a set of expected variables, the INPUT. For each input, a level (OPTIONAL,REQUIRED), and a type (String? Double?) are provided.

The abstract class checks the requirement. If the contract is not respected, then a BPMN error is thrown.

So, when the method execution is called, implementation is sure that all required information is provided

````
public void execute(final JobClient jobClient, final ActivatedJob activatedJob) {
````

On the opposite, the worker declares the list of Output variables it will be created. The abstract class checks that all output variables is correctly produced by the worker, no more, no less. Suppose the output contract is not respected (you forgot one variable, or you provided an undeclared variable), a BPMN error is thrown.

A contract is very useful:
* As a developer, you don't need to worry about the existence of the variable. If you ask it, you will have it during the execution.
* As a designer, all Input and Output variables for a worker are declared and documented.

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
These methods exploit the storageDefinition and save or retrieve the file for the worker.








# User and designer usage

## Introduction

The Cherry framework comes with some workers. You can start it as it is, or it may be embedded in a project to deliver more workers.

The library contains a WebApplication. You can access it if you start the Cherry Framework project or if you start a project which includes the Cherry Framework project

## Specify the connection

The connection to the Zeebe engine is piloted via the application.properties located on src/main/java/resources/application.properties

### Use a Camunda Saas Cloud:

1. Follow the [Getting Started Guid](https://docs.camunda.io/docs/guides/getting-started/) to create an account, a
   cluster and client credentials

2. Use the client credentials to fill the following environment variables:
    * `ZEEBE_ADDRESS`: Address where your cluster can be reached.
    * `ZEEBE_CLIENT_ID` and `ZEEBE_CLIENT_SECRET`: Credentials to request a new access token.
    * `ZEEBE_AUTHORIZATION_SERVER_URL`: A new token can be requested at this address, using the credentials.

3. fulfill the information in the application.properties
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
zeebe.client.security.plaintext=true
````

## Start the application
Execute start.sh or start.bat  available on the framework.

After starting, all workers in the project begin to monitor the Zeebe server.
When a task is ready to be executed by one of the workers, it is processed.

## Access the Web Application

Access the webapp here: http://localhost:9081. 

Currently, there is just a single welcome page that calls the `/cherry/api/workers/list` to show
workers found in system:  

![Web Page Welcome Screen Shot](src/main/resources/static/img/welcomeScreenShot.png?raw=true)

The workers section describes all workers available in the project. For each worker, you have a list of Inputs 
expected and Outputs produced by the worker.

Click on one of the rows in the table to see more details. For example, here are details about the "PingWorker": 

![Worker Detail Screen Shot](src/main/resources/static/img/workerDetailScreenShot.png?raw=true)

Each worker follows the same pattern:
* it declares a type. This type must be used in your process to specify the worker.
By convention, type name is 'c-<collection>-<name>'. For example, the LoadFileFromDiskWorker type is 'c-pdf-convert-to'

* it expects input data
The OfficeToPdf expects to input an MSOffice document.

* it processes the task and produces output data
For example, the OfficeToPdf worker produces a PDF document

* it may produce some BPM errors.
If the conversion to a PDF document fails, a BPMN Error is thrown.

* Check out each collection and worker to see the detail.

## How to integrate a worker into your process?

Check out the definition. Creates a service task and uses the type.

Check the input data. Use the Input facility in Camunda to map the required Input and your process.


For example, the LoadFilesFromDisk worker required a folder and a filename to load a file. Input are "folder" and "fileName" (1).
If you don't have these variables in your process or with a different name (for example, the file name you want to load is under the process variable "ContractDocument"), then an Input is the solution.

* Add an Input with the required name, i.e., fileName
map the value of the Input to your variable:
````
fileName = ContractDocument
````
You can give a constant for Input. If all files are located under c:/document/contract, then give as Input
````
folder = "c:/document/contract"
````

* You proceed in the same way to map the output to your process variables, to link an output to your process variable.

(1) LoadFilesFromDisk has different scenarios to load files. You can load a file explicitly by its name or via a filter like "*.docx"



## Manage files (or documents)

Zeebe does not store files as it.
The Cherry project offers different approaches to manipulating files across workers.
For example, OfficeToPdf needs an MS office or an Open Office document as input and will produce a PDF document as a result.

How to give this document? How to store the result?

The Cherry project introduces the StorageDefinition concept. This information explains how to access files (same concept as a JDBC URL).
Then the worker LoadFileFromDisk required a storageDefinition, and produced as output a "fileLoaded".
Note: the storage definition is the way to access where files are stored, not the file itself.

Existing storage definitions are:
* **JSON**: files are stored as JSON, as a process variable. This is simple, but if your file is large, not very efficient. The file is encoded in base 64, which implies a 20 to 40% overload, and the file is stored in the C8 engine, which may cause some overlap.

Example with LoadFileFromDisk:
````
storageDefinition: JSON
````
fileLoaded contains a JSON information with the file 
````
{"name": "...", "mimeType": "application/txt", value="..."}
````

* **FOLDER:<path>**. File is store on the folder, with a unique name to avoid any collision.

Example with LoadFileFromDisk:
````
storageDefinition: FOLDER:/c8/fileprocess
````
fileLoaded contains 
````
"contractMay_554343435533.docx"
````
Note: the folder is accessible by workers. If you run a multiple Cherry application on different hosts, the folder must be visible by all applications.

* **TEMPFOLDER**, the temporary folder on the host, is used to store the file, with a unique name to avoid any collision

Example with LoadFileFromDisk:
````
storageDefinition: TEMPFOLDER
````
fileLoaded contains 
````
"contractMay_554343435533.docx"
````
This file is visible in the temporary folder on the host

Note: the temporary folder is accessible only on one host, and each host has a different temporary folder. This implies your workers run only on the same host, not in a cluster.



## Example of usages
See different examples under src/test/resources/org.camunda.cherry. You have a folder per collection, and processes in the collection.



