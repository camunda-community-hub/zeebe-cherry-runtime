[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

# Vercors project

The Vercors project is a collection of Workers ready to use for Zeebe.

The project is delivered as a Springboot application. 


A Zeebe server must be started (in CamundaCloud, or onPremise), and the Vercors application will connect to the Zeebe engine.

1. set up the Zeebe connection 
Checks the properties file located in src/main/resources/application.properties.
You can give a CamundaCloud Zeebe

````
# use a cloud Zeebe engine
zeebe.client.cloud.region=
zeebe.client.cloud.clusterId=
zeebe.client.cloud.clientId=
zeebe.client.cloud.clientSecret=
````

Use a onPremise Zeebe
````
# use a onPremise Zeebe engine
zeebe.client.broker.gateway-address=127.0.0.1:26500
zeebe.client.security.plaintext=true
````


2. Compile the application 
````
mvn install
````
3. Starts the Vercors application 
 
The application is located under `src/main/java/org/camunda/vercors/VercorsApplication.java`

This application starts all workers. There is one thread for the application, which can be changed in the application.properties file (parameter zeebe.client.worker.threads)


# How to Use

After starting, all workers in the Vercors project begin to monitor the Zeebe server.
When a task is ready to be executed by one of the workers, it is processed.

Check the different available workers. Workers are grouped by collection. All collections are visible under src/main/java/org/camunda/vercors. 
Each folder is a collection, except for the definition folder.

Each collection/folder contains a README to explain all workers available in a collection in detail.


Each worker follows the same pattern:
* it declares a type. This type must be used in your process to specify the worker.
By convention, type name is 'v-<collection>-<name>'. For example, the OfficeToPdf worker type is 'v-pdf-convert-to'

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

## Connection Setup
The connection to the Zeebe engine is piloted via the application.properties located on src/main/java/resources/application.properties

To connect to a set up a self-manage engine, used the parameter

````
zeebe.client.broker.gateway-address=127.0.0.1:26500
````

Give the correct IP + port number.


### Connect to Cluster Camunda Cloud
The connection to the Zeebe engine is piloted via the application.properties located on src/main/java/resources/application.properties


1. Follow the [Getting Started Guid](https://docs.camunda.io/docs/guides/getting-started/) to create an account, a
   cluster and client credentials

2. Use the client credentials to fill the following environment variables:
    * `ZEEBE_ADDRESS`: Address where your cluster can be reached.
    * `ZEEBE_CLIENT_ID` and `ZEEBE_CLIENT_SECRET`: Credentials to request a new access token.
    * `ZEEBE_AUTHORIZATION_SERVER_URL`: A new token can be requested at this address, using the credentials.
   
3. Run the application

## Manage files (or documents)

Zeebe does not store files as it.
The Vercors project offers different approaches to manipulating files across workers.
For example, OfficeToPdf needs an MS office or an Open Office document as input and will produce a PDF document as a result.

How to give this document? How to store the result?

The Vercors project introduces the StorageDefinition concept. This information explains how to access files (same concept as a JDBC URL).
Then OfficeToPdf requied a sourceStorageDefinition, and a destinationStorageDefinition.
LoadFileFromDisk required a storageDefinition, and produced as output a "fileLoaded".
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
Note: the folder is accessible by workers. If you run a multiple Vercors application on different hosts, the folder must be visible by all applications.

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



## Example of usage
See different examples under src/test/resources/org.camunda.vercors. You have a folder per collection, and processes in the collection.

# How to add a connector

## Principle
Let's create a new connector in the Vercors project. In order to keep the project consistent, some view rules has to be followed.
The model is the collection message (**"org.camunida.vercors.message"**), and the worker **"SendMessageWorker"**.

- The first level of the Vercors project is the collection name. Your connector must be attached in a collection, and may be under a sub collection.
A collection is a package. For example, the worker **"SendMessage"** is under the collection **"message"**.

- The worker must be suffixed by the name **"Worker"**.
Example: Package is **"org.camunda.vercors.message"**, class is **"SendMessageWorker"**.
If this class need another class, it can be saved under the same package, or under a sub package.

- type name Convention: type should start by **v-<collectionName>-** to identify a Vercors connector and don't mixup with different connectors. 
It must follow the snake case convention (https://en.wikipedia.org/wiki/Snake_case) 
Example: **"v-message-send**.

- README in each collection. This file explains all workers present in the collection. For each worker, this information has to be fulfilled:
  - the worker name
  - a description
  - the type
  - Inputs
  - Outputs
  - Errors the connector can throw.


## tests
Java class test follows the same architecture, test/java/org.camunda.vercors.<CollectionName>.

Process test should be saved under test/resources/<collectionName>. For example, the SendMessage.bpmn test process is saved under main/resources/message



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

On the opposite, the worker declares the list of Output variables it will be created. The abstract class checks that all output variables is correctly produced by the worker, no more, no less. Suppose the output contract is not respected (you forgot one variable, or you provided an undeclared variable, a BPMN error is thrown.

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

Vercors project offers a mechanism, the storage definition.
Two methods are available:
````
public FileVariable getFileVariableValue(String parameterName, String storageDefinition, final ActivatedJob activatedJob) {

public void setFileVariableValue(String parameterName, String storageDefinition, FileVariable fileVariableValue) {
````
These methods exploit the storageDefinition and save or retrieve the file for the worker.



## Documentation

