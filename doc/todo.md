# todo

Main new feature are registered via the GitHub issue, but here are additionnal


# Inbound connector
Cherry handle Inbound connector

# Initial Upload

via an environment variable, upload JAR file directly at startup. It helps to deploy quickly any worker/connector.
it's possible to give an HTTP link to access the component, a GitHub project or a connector in the marketplace. 
With a GitHub project, Cherry explore the release attached.


```yaml
cherry:
  initialisation:
    runners:
      - https://github.com/camunda-community-hub/camunda-8-connector-pdf/releases/download/3.1.1/pdf-function-3.1.1.jar
      - https://github.com/camunda-community-hub/camunda-8-connector-zip/releases/download/1.0.0/ZIP-1.0.0.jar
      - https://github.com/camunda-community-hub/connector-8-CMIS
      - https://marketplace.camunda.com/en-US/apps/782060/telegram-connector
````

# GitHub repository

Initialise private GitHub
Cherry explore all projects in this GitHub to search for worker/connector, and propose the list in the Store tab

```yaml
cherry:
  repository:
    - https://github.com/camunda-community-hub/
```

# PVC - Google Drive - S3
Explore PVC, Google Drive, S3 bucket for connectors

To be defined

# MarketPlace 
jar can be uploaded directly from the marketplace


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
