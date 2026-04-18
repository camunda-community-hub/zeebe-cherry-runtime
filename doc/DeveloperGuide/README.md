# Developer guide
the purpose of this document is to explain how to develop with the runtime. The target audience are the developers.

This section focus on the development part, to create a new collection of connector/worker.
The developer can choose two different pattern:
* connector
* worker

The connector pattern embed the Connector SDK
![Architecture](../images/Architecture.png?raw=true)

In the next part of the documentation, we use the term of **Runner**. A Runner is a Connector or a Worker.

The library can be used in a simple worker to create the element-template.



## Connector or Worker?

A Connector is the building block behind every Worker. Conceptually, a Connector is generic and reusable — everything published on the community hub is a Connector. A Worker is more company-specific: it solves a particular problem in your organization's context.
The structural difference follows from this. A Connector receives a dedicated Input class whose members are the connector's parameters, and produces an Output class. This gives a Connector three parts: the function (where the core logic lives), the Input class, and the Output class. A Worker, by contrast, accesses a flat list of variables directly.
That said, the two structures are interchangeable at runtime. The Cherry runtime executes both through the same abstraction — called a Runner — with no distinction between them. You can move from a Worker structure to a Connector structure at any time.
The library provides an abstract class for each:

AbstractConnector implements the OutboundConnectorFunction interface.
AbstractWorker is its counterpart for the worker pattern.

# Integrate the element-template generator in an existing worker

To generate the element template from an existing worker, the method consist:
* to add the Cherry library in your project
* to have one class per worker
* to extend this class by the Cherry implementation
* to create a application to start the generator

Follow the "develop a worker" section

# Maven library

Include the Maven library in your project. check the last version.

```xml
   <dependency>
        <groupId>io.camunda.community</groupId>
        <artifactId>zeebe-cherry-runtime</artifactId>
        <version>3.4.0</version>
   </dependency>
```

# Generate Element template

The generation is call explicitaly, for example in a test package

```java

package io.camunda.worker;

import io.camunda.cherry.definition.RunnerDecorationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementTemplateGenerator {
private static final Logger logger = LoggerFactory.getLogger(ElementTemplateGenerator.class.getName());

    public static void generate() {
        // Call the Cherry runtime
        try {
            RunnerDecorationTemplate decoration = new RunnerDecorationTemplate(new Sample());
            decoration.generateElementTemplate("./element-templates/", "sample-worker.json");
        } catch (Exception e) {
            logger.error("Error during generation", e);
        }
    }

    public static void main(String[] args) {
        generate();
    }
}
```


# Develop a connector

A connector contains three classes:
* the Function
* the Input
* the Output

To not force to onboard any Cherry library for a connector, interface may be used.
These interface can be copied directly in the connector, under `io.camunda.connector.cherrytemplate`. Four interfaces can be copied: `CherryConnector`, `CherryInput`, `CherryOutput` and `RunnerParameter`.

To generate the element template, the maven library must be included, but with the scope "test"



The Function class implements the `CherryConnector` interface. 
```
public class CalendarAdvanceFunction implements OutboundConnectorFunction, CherryConnector 
```

Some methods must be implemented:  
* `getDescription()`, 
* `getLogo()`, 
* `getCollectionName()`
* `getListBpmnErrors()`
* `getInputParameterClass()` to reference the Input class
* `getOutputParameterClass()` to reference the output class
* `getAppliesTo()`

See [Description Method](#description-method) for more information

The Input class must implement the `CherryInput` interface.
```java
public class CalendarAdvanceInput implements CherryInput
```

Method `getInputParameters()` must be defined. This method return the list of parameters.

The Output class must implement the `CherryOutput` interface.
```java
public class CalendarAdvanceOutput implements CherryInput
```

Method `getOutputParameters()` must be defined. This method return the list of parameters.


# Develop a worker

The constraint is to define one class per worker.

There are two level available:
* AbstractRunner. This level ensure to have the different method available to create the element-template, but does not force any additionnal behavior during the execution.  
* AbstractWorker. This level add some additional constraint.

## Abstract runner

This level is recommended to reduce the impact on your worker.

Define a class for your worker. The class extend the AbstractRunner class. 

```
public class MyWorker extends AbstractRunner
```

You will have in your worker:

```java
   @JobWorker(type = JOB_TYPE_SAMPLE_WORKER, fetchVariables = {IN_START_DATE, IN_DIRECTION, IN_UNIT})
    public void handleSampleWorker(final JobClient client, final ActivatedJob job,
                                   @Variable(name = IN_START_DATE) String startDay,
                                   @Variable(name = IN_DIRECTION) String direction,
                                   @Variable(name = IN_UNIT) String unit) {
        logger.info("sampleWorker worker [{}] direction[{}] unit[{}]", startDay, direction, unit);

        // Code implementation ....
        client.newCompleteCommand(job.getKey()).variable(RESULT_DAY_CALCULATION, new Date()).execute();

    }
```

This force the class to override multiple methods.

See [Description Method](#description-method)

The constructor must define [Input declaration](#Input declaration), [Output declaration](#Output declaration) and BPMN Error:
```java

public Sample() {
    super(JOB_TYPE_SAMPLE_WORKER,
            List.of(inputStartDay, inputDirection, inputUnit),
            List.of(outputDayCalculation),
            List.of(new BpmnError("HOLIDAY", "The input day is a holiday")));
}
```

See the Input declaration and Output declaration 



# Develop a worker via AbstractWorker

This level add new function. The handle method should call the handle() defined in the AbstractWorker level. This method will validate the input.
In the Input declaration, some parameter are defined as optional, or required. The AbstractWorker validate the input, to ensure they are correct.
The worker code is simplify, because it does not have to validate input. 
Same as the output: a output parameter marked as REQUIRED must be provided by the worker;

The worker must call the handle() method. The handle() method validate input, and then call the execute() method.
```java

    @JobWorker(type = JOB_TYPE_SAMPLE_WORKER, fetchVariables = {IN_START_DATE, IN_DIRECTION, IN_UNIT})
    public void handleSampleWorker2(final JobClient client, final ActivatedJob job) {
        super.handle(client,job);
    }

    @Override
    public void execute(JobClient jobClient, ActivatedJob activatedJob, ContextExecution contextExecution) {

    }
```



# Cherry definition

The Cherry environment needs some methods.

## description method
This part works for connector and worker.

### isWorker

```
    public boolean isWorker()
```

> This method is needed if you implement AbstractRunner

Return true if the class is considered as a worker.

### isWorker

```
    public boolean isConnector()
```

> This method is needed if you implement AbstractRunner

Return true if the class is considered as a connector.

### getDescription


```
    public String getDescription()
```

Return a description. Will populate the element-template description.

### getLogo

```
    private static final String WORKER_LOGO = "data:image/png;base64,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";
    public String getLogo()
```

Logo must be an SVG image in Base64


## Input declaration

![Inputs](TemplateModeler.png?raw=true)
Give a list of Input that your connector / worker expect.

````
       Arrays.asList(
            RunnerParameter.getInstance("message", "Message to log", String.class, RunnerParameter.Level.OPTIONAL, "Message to log, to ensure the worker was called"),
            RunnerParameter.getInstance("delay", "Delay in ms", Long.class, RunnerParameter.Level.OPTIONAL, "Delay to sleep, in milliseconds")
        ),
````

This information build the documentation and the Element-Template

![InputDocumentation](InputDocumentation.png?raw=true)


## Output declaration

List of parameters returned by the worker


## BPMN Errors

Give the list of BPMN Errors that the connector/worker can throw. This list will be available in the documentation.
The Element-Template will contain the FEEL expression to transform a `ConnectorException` to a BPMN Error.
Without this list, all `ConnectorException` will be transformed into a Fail.

## Runner parameter
Each parameter has:
* a name (message)
* a label (Message to log). This label is visible in the Element Template for example
* a type. Multiple getter are available to access variables
* a scope: OPTIONAL, MANDATORY
* a description

A parameter may have multiple decorators. These are used to generate a rich Element-Template.

### setVisibleInTemplate()
A Mandatory parameter is visible every time.
An Optional parameter has a checkbox. The designer has to select the checkbox to be able to give the value.
With this decoration, the optional parameter is visible at any time (and the modeler force the designer to give a value).


### getAccessAllVariables()
A runner (connector/worker) accesses a list of predefined variables. These variables will be check by the AbstractWorker, to verify that it respect its contract.

The runner may need to have dynamic access: a variable is a prefix (designer says "_green"), and the runner will access the `temparature_green` variable.
When a parameter adds this decorator, then all variables in the process instance will be fetched to the connector/worker.

> If the Spring annotation is used, a limited number of variables can be fetched. This decorator does not override the spring Annotation.

### addCondition()

To condition the visibility of a parameter by another parameter.
Let's say that you have a parameter COUNTRY and a parameter STATE. You want to show the parameter STATE only when COUNTRY equals USA or BRAZIL.
Then, on the parameter STATE, add

````
.addCondition("COUNTRY", Arrays.asList("USA", "BRAZIL"));
````

### addChoice()
A parameter can be a list of choice, and the designer will choose one in the dropdown. Add all choice via this decorator

````
.addChoice("USA", "United State Of America")
.addChoice("Brazil", "Brazil")
.addChoice("FR", "France")
````

## Additional method

AbstractRunner offer a series of `getInput` variable, to help the worker implementation.
Each getInput function need:
* The variable name
* A default value
* The job

For example:

`````
String message = getInputStringValue(INPUT_MESSAGE, null, activatedJob);
Long delay = getInputLongValue(INPUT_DELAY, null, activatedJob);
`````


## Output as a list of variables or as an object?

The output of your connector may be
* a list of parameters. Then you can match one parameter by one in the Element-template, in multiple variables. Or if you don't want a result, just do not match it.
* an object saved in one process variable

**Save the result as an object**
![Output as an object](OutputConnectorObject.png?raw=true)

This is the default behavior.

Just reference the Output object.

````java

public class PingObjectConnectorOutput {

  private final Long internalTimeStampMS;
  private final String internalIpAddress;

  public PingObjectConnectorOutput(long currentTimestamp, String ipAddress) {
    super();
    this.internalTimeStampMS = currentTimestamp;
    this.internalIpAddress = ipAddress;
  }

  /* Return an objet, getter can be regular */
  public long getTimeStampMS() {
    return internalTimeStampMS;
  }

  /* Return an objet, getter can be regular */
  public String getIpAddress() {
    return internalIpAddress;
  }
}
````


**In multiple output parameters**

![Output as a list of variable](OutputConnectorFields.png?raw=true)

Then, you have to specify the list of outputs in the method `getOutputParameters()` and
**You must create a getter for each member, starting with a lower case**
For example, for the object

````
private long internalTimeStampMS;
````

the method must be
````
public long gettimeStampMS() {
````
if the *time* does not start by a lower case, then Zeebe will not find the value and can't do the correct mapping.

`````java
public class PingConnectorOutput extends AbstractConnectorOutput {

  private long internalTimeStampMS;
  private String internalIpAddress;

  private Map<String, Object> parameters;

  public PingConnectorOutput() {
    super();
  }

  public PingConnectorOutput(long currentTimestamp, String ipAddress, Map<String, Object> parameters) {
    super();
    this.internalTimeStampMS = currentTimestamp;
    this.internalIpAddress = ipAddress;
    this.parameters = parameters;
  }

@Override
  public List<RunnerParameter> getOutputParameters() {
    return Arrays.asList(
      RunnerParameter.getInstance("timeStampMS", "Time stamp", Long.class, RunnerParameter.Level.REQUIRED,"Produce a timestamp"),
      RunnerParameter.getInstance("ipAddress", "Ip Address", String.class, RunnerParameter.Level.REQUIRED,"Returm the IpAddress"),
      RunnerParameter.getInstance("parameters", "Parameters", Map.class, RunnerParameter.Level.REQUIRED,"Returm parameters"));
  }

  /* The getter must start by a lower case */
  public long gettimeStampMS() {
    return internalTimeStampMS;
  }

  /* The getter must start by a lower case */
  public String getipAddress() {
    return internalIpAddress;
  }

  public Map<String, Object> getparameters() {
    return parameters;
  }
}
`````
