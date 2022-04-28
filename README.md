[![Community badge: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![Community extension badge](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

# Zeebe Worker Java Template

Minimal template for a [Zeebe](https://github.com/camunda-cloud/zeebe)
Java [worker](https://docs.camunda.io/docs/components/concepts/job-workers/). This template adds only the bare minimum
of dependencies.

If you want something more convenient, have a look
at [Spring Zeebe](https://github.com/camunda-community-hub/spring-zeebe).

## How to Use

The main method is in `Worker.java`. It requires a couple of environment variables to run.

### Connection Setup

#### Connect to Cluster Camunda Cloud

1. Follow the [Getting Started Guid](https://docs.camunda.io/docs/guides/getting-started/) to create an account, a
   cluster and client credentials
2. Use the client credentials to fill the following environment variables:
    * `ZEEBE_ADDRESS`: Address where your cluster can be reached.
    * `ZEEBE_CLIENT_ID` and `ZEEBE_CLIENT_SECRET`: Credentials to request a new access token.
    * `ZEEBE_AUTHORIZATION_SERVER_URL`: A new token can be requested at this address, using the credentials.
3. Run `Worker`

#### Connect to local Installation

For a local installation (without authentication) you only need to set `ZEEBE_ADDRESS`

### Workflow

Either you deploy `process.bpmn` or you design your own process with a service task with the `greet` job type.


