#Introduction
This document explains how to embed an existing connector. It gives some tips for creating a connector.


# Build a connector

## Main rules
An existing connector does not reference any external framework. The Cherry Framework must not
be referenced.

A connector can reference filestorage library, to manipulate files.

```
 <!-- Access file -->
    <dependency>
      <groupId>io.camunda.filestorage</groupId>
      <artifactId>filestorage</artifactId>
      <version>${filestorage.version}</version>
    </dependency>
```


A connector must define a license: copy the LICENSE file directly at the root level

In the READ ME, the structure must be, per connector:

### Level 1: Type Of Connector

### Level 1: Build

#### Level 2: API

##### Level 3: Input
The JSON structure must be added, with all needed explanation

##### Level 3: Output
The JSON structure must be added, with all needed explanation

##### Level 3: Errors
Different BPMN errors that the connector can produce sould be referenced

#### Level 2: Element templates
Give the path where the element template is


Visit https://github.com/camunda-community-hub/camunda-8-connector-officetopdf to have an example.


## Include dependencies

The two dependencies to include is
`````
   <dependency>
    <groupId>io.camunda.connector</groupId>
    <artifactId>connector-core</artifactId>
    <version>${connector-core.version}</version>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>io.camunda.connector</groupId>
    <artifactId>connector-validation</artifactId>
    <version>${connector-validation.version}</version>
    <scope>provided</scope>
  </dependency>
`````

## Name the package
Name the package io.camunda. Connector.<ConnectorName>
Example: io.camunda. Connector.officetopdf


## Create three classes

Create three java classes to manipulate the Connector

* <connectorName>Function is the core of your Connector
* <connectorName>Input stores all input information
* <connectorName>Output stores all output information

## Function class
The @OutboundConnector must be used to create your Connector:

`````

@OutboundConnector(name = "OfficeToPdf", 
    inputVariables = { OfficeToPdfInput.INPUT_SOURCE_FILE_VARIABLE,
      OfficeToPdfInput.INPUT_DESTINATION_FILE_NAME,
      OfficeToPdfInput.INPUT_DESTINATION_STORAGEDEFINITION }, 
    type = "c-pdf-convert-to")

public class OfficeToPdfFunction implements OutboundConnectorFunction {

  /**
   * Different BPMN Errors this Connector can throw
   */
  public static final String BPMERROR_CONVERSION_ERROR = "CONVERSION_ERROR";
  public static final String BPMERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";

  /**
   * Topic for this connector
   */
  public static final String WORKERTYPE_PDF_CONVERT_TO = "c-pdf-convert-to";

  /**
   * Different inputs
   */
  private static final String INPUT_SOURCE_FILE = "sourceFile";
  private static final String INPUT_DESTINATION_FILE_NAME = "destinationFileName";
  private static final String INPUT_DESTINATION_STORAGEDEFINITION = "destinationStorageDefinition";
  /**
   * Different outputs
   */
  private static final String OUTPUT_DESTINATION_FILE = "destinationFile";


`````

Declare the constant in the Input class Object
`````

public class OfficeToPdfInput {

  public static final String INPUT_SOURCE_FILE_VARIABLE = "sourceFileVariable";
  @NotEmpty String sourceFileVariable;

  public static final String INPUT_DESTINATION_FILE_NAME = "destinationFileName";
  @NotEmpty String destinationFileName;


  public static final String INPUT_DESTINATION_STORAGEDEFINITION = "destinationStorageDefinition";
  String destinationStorageDefinition;


`````

Cherry Framework will require the name of Input and Output to build the templates and documentation.
To be consistent, create in this class a constant per Input: if the name change, then the constant will
change, and you will be able to detect it via a compilation error.


**Attention**, you have to copy the input name in the Spring Annotation.
`````
private static final String INPUT_SOURCE_FILE = "sourceFile";
`````

sourceFile must be referenced in the annotation:
`````
inputVariables = { OfficeToPdfInput.INPUT_SOURCE_FILE_VARIABLE,`````
`````



`````
public static final String BPMERROR_CONVERSION_ERROR = "CONVERSION_ERROR";
`````

Create your method

`````
@Override
public OfficeToPdfOutput execute(OutboundConnectorContext context) throws Exception {
OfficeToPdfInput officeInput = context.getVariablesAsType(OfficeToPdfInput.class);

}
`````
## Throw errors
Generate a ConnectorException to throw an error.
The Connector Exception needs a code: give a constant as defined.

`````

public static final String BPMERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";


...
    
      throw new ConnectorException(BPMERROR_LOAD_FILE_ERROR,
          "Connector [" + getName() + "] cannot read file[" + INPUT_SOURCE_FILE + "]");

`````
The ConnectorException will be catched by the Cherry facade.

## Access files
C8 does not manipulate Document/files.
The library connector-8-DocumentRepository allows these operations. This library is outisde the Cherry Framework
   
`````   
    <dependency>
      <groupId>io.camunda.connector</groupId>
      <artifactId>zebee-cherry-filestorage</artifactId>
      <version>1.0.0</version>
      <scope>provided</scope>
    </dependency>
`````
   

Documents/files can be saved in different storage: File system, CMIS, and Database.
   
To save a file in the repository, use this method (note that the storageDefinition String is asked as a Input):

````
    byte[] content;

    StorageDefinition storageDefinition = StorageDefinition.getFromString( storageSt );

    FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();
    FileVariable doc = fileRepoFactory.createFileVariable(storageDefinition);

    doc.setValue( content );
    doc.setName( "MyFileName");
    FileVariableReference docReference = fileRepoFactory.saveFileVariable(doc);

````
The docReference can be saved in a process variable as a String (to avoid any Library issue)

````
  String docReference = docReference.toJson()
````

To read a file from the repository with the docReference, use:

````
    FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();
    FileVariable doc = fileRepoFactory.loadFileVariable(FileVariableReference fileVariableReference) throws Exception {
````

## Generate the element template
You can use the Cherry Framework to generate the element template.

Branch the Connector inside a Cherry collection, start the application, and via the administration, Cherry generates the element-templates.

## Output object in the template
You have to choose how to produce the result in the element-template.

In both situations, the Connector produces an Object in the output. But in the Modeler, you may display just a field,
and the designer will select the process variable where the Object is saved.

Or in the Modeler, you may want to present one field per property of your Object. The ConnectorSDK has a constraint, then.

In our example, the Object returned is

````
public class PingObjectConnectorOutput  {

    private Long internalTimeStampMS;
    private String internalIpAddress;

````

* Object output

A result is an object. In the Modeler, the template present this:
![Object Output](OutputConnectorObject.png?raw=true)

By doing that, you don't have any particular constraints on the Object. Different members (internaltTimeStampMS) may be public,
or private, and any getter can be defined.

````
public long getTimeStampMS() {
    return internalTimeStampMS;
}

public String getIpAddress() {
    return internalIpAddress;
}
````

The Element Template is:
````
{
    "label": "Result Variable",
    "binding": {
        "type": "zeebe:taskHeader",
        "key": "resultVariable"
    },    
    "type": "String",
    "value": "result"
    },
````

* Fields output
  If you want to present in the Modeler the different fields, and user can setup each property in a differ
  ![Fields Output](OutputConnectorFields.png?raw=true)

Then, the Connector must expose a list of getter(). But the first letter in the getter must be in *lower case* else
the connector SDK will not find it

````

public long gettimeStampMS() {
return internalTimeStampMS;
}

public String getipAddress() {
return internalIpAddress;
}
````
If you define a getter like getTimeStampMS, then the execution will fail. After the first letter, any Upper Case can be used.

The Element Template is
````
{   
    "binding": {
        "type": "zeebe:taskHeader",
        "key": "resultVariable"
    },
    "type": "Hidden",
    "value": "result"
},
{
    "description": "Produce a timestamp",
    "binding": {
        "type": "zeebe:output",
        "source": "= result.timeStampMS"
    },
    "id": "timeStampMS",
    "label": "Time stamp",
    "type": "String",    
},
{
    "description": "Returm the IpAddress",
    "binding": {
        "type": "zeebe:output",
        "source": "= result.ipAddress"
    },
    "id": "ipAddress",
    "label": "Ip Address",
    "type": "String",    
}
````

The tip is in the FEEL expression "= result.timeStampMS".
To access the information `result.myField`, Feel will call a method `getmyField`.
It is forbidden to set in a FEEL expression `result.MyField`: the first letter must be in lowercase.

So, if you want to generate an Element Template with one Input for each member of the output, you must create a getter according to the constraints.





## Good tips

Create a method getName() to return the name of the Connector.






# Camunda Nexus repository
To be visible to all pom.xml and to be integrated, the Connector must be registered in the Camunda Nexus Repository.

In the meantime, execute a mvn install on your repository. The JAR file is saved on your local maven repository and will be available for integration into a collection.

The procedure is visible here
https://github.com/camunda-community-hub/community-action-maven-release

## Add the correct parent in the pom.xml

To deploy the library, the parent in pom.xml must be


```
  <parent>
    <groupId>org.camunda.community</groupId>
    <artifactId>community-hub-release-parent</artifactId>
    <version>1.3.1</version>
  </parent>
```

## Define a GitHub workflow
Under the root of your project, create a directory `.github`, and then a directory, `workflows`.
Copy from this GitHub repository the three different files `deploy.yaml`, `mvn-build.yml`, `mvn-release.yml`

Copy `dependabot.yml` too.

![Github Workflow](GithubWorkflow.png?raw=true)



After each commit in the main, a new workflow should start
![Github Actions](GithubAction.png?raw=true)



## Reference the project in the infrastructure

To allow the project to be deployed, your project must be referenced in this file.

https://github.com/camunda-community-hub/infrastructure

Add your project terraform/github/repositories.tf via a Pull Request.
For example, add at the end

````
"zeebe-cherry-framework",
"connector-8-officetopdf
],
````
Create a pull request with the modification


![Pull request in Infrastructure](AddProjectInInfrastuctureRepository.png?raw=true)

Do not forget to add a comment and tag @camunda-community-hub/devrel.
   
   
# Build a Cherry Collection
A connector is running in a collection. A collection groups multiple runners (connectors and workers).



# reference the Connector in the Cherry Collection
To branch an existing connector to Cherry, you have to create some Facade objects.

A Cherry runner (a connector or framework) requires more information than what is provided in a connector.
This additional information is useful for generating the documentation and the element-template. For example, a connector can get a STRING for a parameter, but as a developer, you want to specify that the String is a list of values (UP, DOWN, RIGHT, LEFT).
Doing that:
* The documentation is more explicit
* The element-template will not be a String, but a list of values (or a reference to a process variable)
* Cherry will verify, before calling the Connector, that the specification is respected


For a connector, you have to path to integrate it.


**Explicit path**

One java class, which describes all the Input and Output parameters. This is more simple (only one class),
but you have to explicitly declare all information. For example, the input Parameter INPUT_FILENAME must be declared as a String class.
With the introspection path, Cherry will load the Java definition and find by itself all parameters because they are members of the class.

Even in the introspection, you will have to declare more information. Cherry required Input to have a label and a description. You may want to add some control.

**Introspection path**
Three java classes. One for the execution, one to declare all Input parameters, and one to declare all Output parameters.

Attention: the introspection method does not work to integrate an existing connector because the Input class must extend the AbstractConnectorInput, and must extend the Input class of the Connector, and this is not possible in Java to extend 2 classes.




## Maven
The first step, the connector JAR must be referenced in the Pom.xml

The collection must reference the maven:
````
  <properties>
    <java.version>17</java.version>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <maven.compiler.source>${java.version}</maven.compiler.source>

    <zeebe.version>8.1.4</zeebe.version>
    <connector-core.version>0.3.0</connector-core.version>
    <connector-validation.version>0.3.0</connector-validation.version>

    <cherry.version>2.1.0</cherry.version>
    <cherryofficepdf.version>1.0.0</cherryofficepdf.version>

    <junit.jupiter.version>5.9.1</junit.jupiter.version>
    <opensagres.version>2.0.3</opensagres.version>
    <spring.boot.version>2.7.4</spring.boot.version>
  </properties>
  ````
And in the dependencies section:

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

    <!-- Cherry Framework -->
    <dependency>
      <groupId>org.camunda.community</groupId>
      <artifactId>zeebe-cherry-framework</artifactId>
      <version>${cherry.version}</version>
    </dependency>
````


## Explicit Path

In the Explicit path, the class declares all the Input Parameters as a list of parameters. Same for the Output Parameters.

Create one class, and name it Facade to be explicit.

The class must extend the AbstractConnector class.

````

import org.camunda.cherry.definition.AbstractConnector;

@Component
public class OfficeToPdfOutputFacade  extends AbstractConnector 
````

ATTENTION: The Cherry Framework automatically detects all runners to start. This detection is based on the following:
* The class is a @Component
* The package starts by io.camunda

If one of the two conditions is not respected, the runner is not detected.

Monitor the log at the start:


The Connector publishes some constants, and this is a good moment to use them.

````
import io.camunda.connector.officetopdf.OfficeToPdfFunction;


public OfficeToPdfFunctionFacade() {
    super(OfficeToPdfFunction.WORKERTYPE_PDF_CONVERT_TO, Arrays.asList(
            RunnerParameter.getInstance(OfficeToPdfFunction.INPUT_SOURCE_FILE, 
                "Source file",
                Object.class,
                RunnerParameter.Level.REQUIRED, 
                "FileVariable for the file to convert"),
            
            RunnerParameter.getInstance(OfficeToPdfFunction.INPUT_DESTINATION_FILE_NAME, 
                "Destination file name",
                String.class, RunnerParameter.Level.REQUIRED, 
                "Destination file name"),
                
            RunnerParameter.getInstance(OfficeToPdfFunction.INPUT_DESTINATION_STORAGEDEFINITION,
                "Destination storage definition", 
                String.class,
                StorageDefinition.StorageDefinitionType.JSON.toString(),
                RunnerParameter.Level.OPTIONAL, "Storage Definition used to describe how to save the file")

        ), Collections.singletonList(
            RunnerParameter.getInstance(OfficeToPdfFunction.OUTPUT_DESTINATION_FILE, 
                "Destination file", 
                Object.class,
                RunnerParameter.Level.REQUIRED, 
                "FileVariable converted")),
        Arrays.asList(BpmnError.getInstance(OfficeToPdfFunction.BPMERROR_CONVERSION_ERROR, 
                "Conversion error"),
            BpmnError.getInstance(OfficeToPdfFunction.BPMERROR_LOAD_FILE_ERROR, 
                "Load File error")));
  }

``````

**Input parameters**

For each input parameter, more information must be provided.
* The label for the Input. This label will be used in the Element-template
* The java type (String? Integer? ). This type must be the same as the Input class.
* The level: REQUIRED? OPTIONAL? When the Connector input has a "@NonEmpty" annotation, it must be turned to REQUIRED
* the explanation on the Input

You can take advantage of the Cherry framework to go deeper into the definition

To define a list of choices, use the addChoice() method
`````
  RunnerParameter.getInstance(INPUT_WATERMARK_COLOR, 
      "Color", 
      String.class,
      RunnerParameter.Level.OPTIONAL, 
      "Color to display the watermark")
    .addChoice("RED", "red")
    .addChoice("GREEN", "green")
    .addChoice("BLACK", "black")
    .addChoice("BLUE", "blue")
    .addChoice("CYAN", "cyan")
    .addChoice("GRAY", "gray")
    .addChoice("DARKGRAY", "darkGray")
    .addChoice("LIGHTGRAY", "lightGray")
    .addChoice("MAJENTA", "magenta")
    .addChoice("ORANGE", "orange")
    .addChoice("PINK", "pink")
    .addChoice("WHITE", "white")
.addChoice("YELLOW", "yellow"),
`````

A condition may be added to an input. If another field's value is taken, the Input field will be visible.
`````
  .addCondition(INPUT_STORAGEDEFINITION, 
              Collections.singletonList(StorageDefinition.StorageDefinitionType.CMIS.toString()))
  
`````

The Input may be registered in a Group.

`````
  .setGroup(GROUP_STORAGE_DEFINITION)
`````

Multiple other operations exist on the Input definition. Refer to the documentation.

**Output parameter**
All Output parameters must be defined too.

**Define the errors**
It is important to register all BPMN Errors that the Connector can throw with an explanation.

**Additional methods**
The Cherry Framework required additional information from the connectors.

Name, description, and Logo to help the administrator create a nice element-template

`````
 @Override
public String getName() {
    return "Office to PDF";
    }

@Override
public String getDescription() {
    return "A PDF document is generated for an office (Word, OpenOffice) document";
    }

@Override
public String getLogo() {
    return WORKERLOGO;
    }
`````

**execute**
The execution is quite similar to the Connector. It will call the Connector.

The ConnectorExecution exception must be caught, and a ZeebeBpmnError can be sent instead if you like.


`````
@Override
public OfficeToPdfOutput execute(OutboundConnectorContext context) throws Exception {
    OfficeToPdfFunction officeToPdfFunction = new OfficeToPdfFunction();
    try {
        return officeToPdfFunction.execute(context);
    } catch (ConnectorException e) {
        throw new ZeebeBpmnError(e.getErrorCode(), e.getMessage());
    }
}
`````

## Introspection path
Three class has to be defined. One for the function, one for the Input parameters, and one for the Output parameter

The construction for the function is different:

````
super(SendBPMNMessageFunction.WORKERTYPE_SEND_MESSAGE,
    SendBpmnMessageInputFacade.class,
    SendBpmnMessageOutputFacade.class,
    Arrays.asList(
        BpmnError.getInstance(SendBPMNMessageFunction.BPMNERROR_TOO_MANY_CORRELATION_VARIABLE_ERROR,
            "Correlation error. The Correlation expects one and only one variable."),
        BpmnError.getInstance(SendBPMNMessageFunction.BPMNERROR_INCORRECT_VARIABLE,
            "A variable must <name>=<value>")));
````

The list of parameters is moved to the Input class.

**Input class facade**
The Input declares all members and still need to return the declaration for each Input in the method getInputParametersInfo() :

````
public class SendBpmnMessageInputFacade extends AbstractConnectorInput {


public static final String INPUT_MESSAGE_NAME = "messageName";
public static final String INPUT_CORRELATION_VARIABLES = "correlationVariables";
public static final String INPUT_MESSAGE_VARIABLES = "messageVariables";
public static final String INPUT_MESSAGE_ACCESS = "*";
public static final String INPUT_MESSAGE_ID_VARIABLES = "messageId";
public static final String INPUT_MESSAGE_DURATION = "messageDuration";

/**
* Return the parameters definition
*
* @return list of parameters
  */
  @Override
  public InputParametersInfo getInputParametersInfo() {
    List listParameters= Arrays.asList(
        RunnerParameter.getInstance(INPUT_MESSAGE_NAME, "Message name", String.class, RunnerParameter.Level.REQUIRED, "Message name"),
        RunnerParameter.getInstance(INPUT_CORRELATION_VARIABLES, "Correlation variables", String.class, RunnerParameter.Level.OPTIONAL, "Correlation variables. The content of these variables is used to find the process instance to unfroze"),
        RunnerParameter.getInstance(INPUT_MESSAGE_VARIABLES, "Message variables", String.class, RunnerParameter.Level.OPTIONAL, "Variables to copy in the message"),
        RunnerParameter.getInstance(INPUT_MESSAGE_ID_VARIABLES, "ID message", String.class, RunnerParameter.Level.OPTIONAL, "Id of the message"),
        RunnerParameter.getInstance(INPUT_MESSAGE_ACCESS, "*", String.class, RunnerParameter.Level.OPTIONAL, "Access any variables referenced in the Correlation of the Message list"),
        RunnerParameter.getInstance(INPUT_MESSAGE_DURATION, "Duration (FEEL duration, String like 'PT1S', or time in ms)", Object.class, RunnerParameter.Level.OPTIONAL, "Message duration. After this delay, message is deleted if it doesn't fit a process instance"));

    return new InputParametersInfo(listParameters, SendBPMNMessageInput.class);
  }

}
````
