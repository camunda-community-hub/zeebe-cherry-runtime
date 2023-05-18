# Docker compose

This section shows different docker compose example. Feel free to copy and adapt them to your use case

There is two flavor:
* the default one, using a H2 database
* the post

## H2 database

Start a complete cherry plus zeebe platform

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-alone.yaml up -d 
```

To remove the different container created (use `stop` instead of `down` if you just want to stop and don't remove the container)

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-alone.yaml down -v
```


## Postgres

This example used a postgres database to store information (statistics, upload connectors)

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-postgres.yaml up -d 
```

To remove the different container created (use `stop` instead of `down` if you just want to stop and don't remove the container)

```shell
docker-compose -f docker-compose-core.yaml -f docker-compose-postgres.yaml down -v
```
