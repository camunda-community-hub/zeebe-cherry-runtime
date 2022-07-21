## TODO

### Administration (UI)
* Build the UI
* Monitoring page:
  First graph, same as Operate, number of active worker / number of stopped worker?
  Second graph, in the last 24 hours, number of correct execution, number of errors?
  List of workers, for each worker, status (actif/not actif), number of threads, number of execution in the last 24 hours, average time execution, longuest execution
  List of errors : display errors in the last 24 hours
* Dashboard page
  for each worker, start/stop it
* documentation page
  for each worker, page which display the description, input, output, BMPN Error, examples?

### Library
Add a function to build a ZeebeBpmnError, instead to let each workers built its own object

add functions to support the administration page

## Version 1.0.1-SNAPSHOT, June 2022

-  added basic index.mustache page with bulma, jquery, and font-awesome


# Version 1.0.0, June 2022

- first version with sample workers