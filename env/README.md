In order to start the application in docker containers you will need to perform the following steps:

1. Build the application by running `./gradlew clean installDist` in the root directory of the project.
2. Build the docker images by running `docker-compose build` in the `/env` directory.
3. Configure the environment variables in the .env file
    * RegionConnectors:
        * add the needed configuration for the region connectors you want to start. Sample config can be found in the
          specific region connector example config files.
    * Kafka:
        * If you run Kafka via docker, you will need to provide the Gateway's IP address. This can be found by
          inspecting
          the container running the Kafka broker. For example by running `docker container ls` to get the id of the
          container and then inspecting the container with `docker inspect <container_id> | grep Gateway`.

4. Start the application by running `docker-compose up -d` in the root directory of the project.
5. Access the application via `http://localhost:8080/demo` in your browser.

If `.env` is configured correctly, the application should start, and you should be able to use all configured region
connectors.

## Run with the example app

From this directory (`eddie/env`), start EDDIE and the example app together:

```shell
docker compose --profile example up -d
```

The `example` profile starts the normal EDDIE services (EDDIE, PostgreSQL, Kafka, and EMQX)
plus Keycloak, the example app, and a dedicated TimescaleDB database for the example app.
It uses the configuration in this repository and does not require the example-app checkout.

Open the example app at <http://localhost:8082>.

To also run AIIDA, start EDDIE first, then run `docker compose up -d` from `../aiida/docker`.
AIIDA joins the same external network and its Keycloak uses port `8889`.
