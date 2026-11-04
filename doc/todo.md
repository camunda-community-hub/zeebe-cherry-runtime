# todo

Main new feature are registered via the GitHub issue, but here are additionnal


# Inbound connector
Cherry handle Inbound connector

# Initial Upload


# Job active tracker
With the new 8.8 API, it's possible to know the number of active job for a specific topic.
```
GET /jobs/statistics/global?jobType=payment-processor
```
https://docs.camunda.io/docs/apis-tools/orchestration-cluster-api-rest/specifications/search-jobs/


Cherry search for the number of active jobs every x seconds (default: 5 seconds). 
* The dashboard display in the current curb this graph: in the same graph with the execution in time, a new value is the number of active job waiting
* A threshold can be configured for the active job, per topic. When the number oversize the threshold, an alert show up
* the current number of job waiting is displayed just after the current statistics
* a dynamic scale can be set up: each topic can have a minum thread/maximum thread. if the number of active job < number of thread, then the number can decrease. Else, it will increase.


# Provide telemetry
ConnectorRuntime provide meter, which can be exploded by Grafana.
Same information should be provided by CherryRuntime
