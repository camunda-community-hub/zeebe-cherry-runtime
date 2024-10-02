# Introduction
This directory contains a help deployment to deploy Cherry runtime on 2 pods, using a Postgres database.

Note: this deployment is a tentative


# Deployment
Check the deployment file, and adapt it.

## Replica
the replica is set to 2 by default, multiple can be set

## Shared folder
A shared folder is created. Connectors can be deployed in this folder, to be uploaded.

## Database
A Postgres database is used. it can be replaced by a different database, or an H2 (then each pod will have it's local database)

## Connection to Zeebe
The deployment must be run in the same cluster as Zeebe. If it is a different cluster, then the zeebe connection must be adapted

# Operation

```shell
kubectl create -f cherry-runtime.yaml
```

kubectl port-forward svc/camunda-zeebe-gateway 26500:26500 -n camunda
