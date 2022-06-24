# Message workers

These connectors groups a set of operation on messages.

# SendMessage

This connector sends a message, in order to create a new process instance, or to correlate an existing process instance.

````
type: v-send-message
````

## Input

| Name                 | Type    | Level                      | Description                                                                                                                                         | Example              |
|----------------------|---------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| messageName          | String  | Required                   | Name of the message to send                                                                                                                         | payment-received     |
| correlationVariables | String  | Optional                   | List of names(2) to describe the correlation (1)                                                                                                    | requestId            |
| messageVariables     | String  | Optional                   | list of names (2) to set the content of the message.                                                                                                | firstName, lastName  |
| messageId            | String  | Optional                   | the unique ID of the message; can be omitted. only useful to ensure only one message with the given ID will ever be published (during its lifetime) ||     |
| messageDuration      | Optinal | Duration or Long or String | how long the message should be buffered on the broker. Long is a value in millisecond, String follow the ISO8601 Notification (3)                   | "PT5M" for 5 minutes |


(1): Camunda 8 accept only one correlation value. The list must contains only one item, and the value must be a
String.||

(2) : list is use the comma to separate information. Example, firstName,lastName

(3) Visit https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-

and https://fr.wikipedia.org/wiki/ISO_8601 to get information on the Duration string

## Output

No output.
