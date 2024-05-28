# Cherry template

Your connector will not include any Cherry library to be fully independant, but when it will be
loaded in the Cherry Runtime, you want to display a lot of information

So, these template are for you.

Copy the package in your project, and implement each interface.

# CherryConnector

Implement this interface. Then, the runtime will be able to collect all information. At minumum,
provide

* name
* description
* logo
* the class where all Inputs are described
* the class where all Outputs are described

# CherryInput

Provide information on Input Parameters. For example, if the field is a list of choice, provide the
list of options

# CherryOutput

Same with the Output.