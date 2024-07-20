# Docker compose

This section shows different docker compose example. Feel free to copy and adapt them to your use case

* [docker-compose-core.yaml](docker-compose-core.yaml) contains the basic to start a Camunda 8 cluster. 
This file is a copy from https://github.com/camunda/camunda-platform/blob/main/docker-compose-core.yaml
* [docker-compose-cherry.yaml](docker-compose-cherry.yaml) is a configuration based on H2
* [docker-compose-cherry-postgres.yaml](docker-compose-cherry-postgres.yaml)





# H2 database

Start a complete cherry plus zeebe platform

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-cherry.yaml -p c8-5-cherry up -d 
```

To remove the different container created (use `stop` instead of `down` if you just want to stop and don't remove the container)

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-cherry.yaml down -v
```


# Postgres

This example used a postgres database to store information (statistics, upload connectors)

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-cherry-postgres.yaml -p c8-5-cherry up -d 
```

To remove the different container created (use `stop` instead of `down` if you just want to stop and don't remove the container)

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-cherry-postgres.yaml down
```

# Onboard Connector JAR file

To start Cherry runtime and onboard automatically 

Check [docker-compose-cherry-postgres-localjar.yaml](docker-compose-cherry-postgres-localjar.yaml)

Place in the jarconnectors any connector, and start the docker compose

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-cherry-postgres-localjar.yaml up -d
```
