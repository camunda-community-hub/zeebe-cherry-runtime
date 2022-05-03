# Message workers

Theses connectors groups a set of operation on messages.

# SendMessage
This connector sends a message, in order to create a new process instance, or to correlate an existing process instance.

````
type: v-send-message
````

## Input

|Name|Type|Description|Example|
|urlMessage|String| Url to send the message|
|messageName|String|Name of the message to send|
|correlationVariables|
|variables|String|list of names (comma separation) to describe the list of variable to send in the message.|


     WorkerParameter.getInstance("urlMessage", String.class, Level.REQUIRED),
                        WorkerParameter.getInstance("messageName", String.class, Level.REQUIRED),
                        WorkerParameter.getInstance("correlationVariables", String.class, Level.OPTIONAL),
                        WorkerParameter.getInstance("variables", String.class, Level.OPTIONAL)),
   
