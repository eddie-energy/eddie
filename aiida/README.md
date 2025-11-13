# ![AIIDA - Administrative Interface for In-house Data Access](docs/images/aiida-horizontal.svg)

The Administrative Interface for In-house Data Access (AIIDA)
connects to various metering devices such as smart meters and home automation systems,
to stream near real-time energy data and other data to consumers like the EDDIE Framework.

To learn more about AIIDA, you can visit its [framework documentation](https://architecture.eddie.energy/aiida) and its [architecture documentation](https://architecture.eddie.energy/architecture/aiida/aiida.html).
If you want to contribute to this repository, please take a look at our [contributing guide](../CONTRIBUTING.md).

## Prerequisites

In order for AIIDA to run, it is necessary to start a [TimescaleDB](https://www.tigerdata.com/timescaledb) and a [Keycloak](https://www.keycloak.org/) instance.
The predefined docker-compose.yml for starting those services can be found in the [docker](docker) folder.
To use data sources that use MQTT, an [EMQX](https://www.emqx.com/) instance is required additionally, which is also included in the
docker-compose.yml.
Before starting the services, the environment variables in the [.env](docker/.env) file should be configured,
especially the `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
This user will be used to authenticate to the TimescaleDB.

### Run with Gradle

For local development, the ports for the TimescaleDB must be exposed in order for the local AIIDA instance to connect to
it, while the default Keycloak configuration is sufficient.
AIIDA has to be configured over the [application.properties](src/main/resources/application.yml) file.
Especially the `spring.datasource` properties have to be configured according to TimescaleDB.

- `./gradlew bootRun`

The permissions REST API will be exposed on the [default Spring Boot port (localhost:8080)](http://localhost:8080)
The web UI is deployed with the Spring application on the same port.
For local development, the web UI can be run separately from its own folder:

```shell
cd ui
pnpm run dev
```

Instead of the environment variables provided to the Spring application, the local development server will use a
separate set of environment variables which is defined in [`ui/.env`](ui/.env).
The default configuration assumes the Docker Compose setup.

### Run with Docker

See in the [AIIDA Operation Manual](https://architecture.eddie.energy/aiida/1-running/OPERATION.html).