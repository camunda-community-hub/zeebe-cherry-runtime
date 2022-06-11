# Operations collection

Workers do some simple operations on the flow

# SetVariables
type: v-set-variables

Worker set variables, from constants or another method
parameters: operation
The value is a list of operation. Each operation is separate by a ;
(<variable>=<value>;)*
for example:
userWalterBatesVar=\"Walter.Bates\";listUsersVar=\"Walter.Bates,Helen.Kelly\";groupQualityVar=\"Quality\"

If the value start by a semi colon, then this is a String. Else, the worker will try to parse the value as Integer, Long, Double.
If the value is on the form "<string>(<parameter>,<parameter>...)" then it will suppose this is a function.
Available function are
````
Date("now"|<Date_ISODATE_FORMAT>)
DateTime("now"|<Date_ISODATETIME_FORMAT>)
localdate("now"|<Date_ISODATETIME_FORMAT>)
LocalTime("now"|<Date_ISODATETIME_FORMAT>)
ZonedDateTime("now"|<Date_ISODATETIME_FORMAT>)
````
For example

````
DateTime(\"now\")
````

return the current Date on the system

````
LocalTime("2022-02-12T01:23:34")
````
return a LocalTime object
You can reference variable for parameters
````
LocalTime(dateOfBirth)
````
assuming the dateOfBirth is a String at the ISO format.