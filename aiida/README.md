# AIIDA - Administrative Interface for In-house Data Access

Development & Deployment Strategy can be
found [here](https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy).

## Prerequisites

In order for AIIDA to run, it is necessary to start a [TimescaleDB](https://www.timescale.com/) and a [Keycloak](https://www.keycloak.org/) instance. The predefined docker-compose.yml for starting those services can be found in the [docker](docker) folder.
In order to use Datasources that use MQTT a NanoMQ instances is required additionally, which is also included in the docker-compose.yml.
Before starting the services, the environment variables in the [.env](docker/.env) file should be configured, especially the
`SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD`. This user will be used to authenticate to the TimescaleDB.

### Run with Gradle

For local development, the ports for the TimescaleDB must be exposed in order for the local AIIDA instance to connect to it, while the default KeyCloak configuration is sufficient.
AIIDA has to be configured over the [application.properties](src/main/resources/application.yml) file. Especially the
`spring.datasource` properties have to be configured according to TimescaleDB.

- `./gradlew bootRun`

The permissions REST API will be exposed on the [default Spring Boot port (localhost:8080)](http://localhost:8080)

### Run with Docker

AIIDA is also available as a docker image at
the [GitHub registry](https://github.com/eddie-energy/eddie/pkgs/container/aiida).
The necessary configuration should be specified via [environment variables](docker/.env), and
an [example docker compose file](docker/compose.yml)
includes a TimescaleDB and Keycloak.
Once started, you can access the AIIDA Web-UI at the default location: http://localhost:8080

## Authentication with Keycloak

The [example docker compose file](docker/compose.yml) includes a keycloak instance with a preconfigured user named
`aiida`, with the password `aiida`.

The preconfigured keycloak includes an EDDIE realm with the AIIDA client, that is used for authentication.
The client secret of the AIIDA client is set to `REPLACE_ME` and can be regenerated in the admin console, which is
reachable at http://localhost:8888.
The keycloak admin user is configured in the [.env](docker/.env) file and has by default the username `admin` and the
password `admin`.

If a different keycloak instance should be used, it can be configured in the
[application.yml](src/main/resources/application.yml) file or using environment variables.

When AIIDA is started locally for development it can lead to unexpected logouts, since both the example app and AIIDA use the same session ID (JSESSIONID) per default.
To overcome this issue, the property `server.servlet.session.cookie.name` can e.g. be set to
`AIIDA_SESSION_ID`, which will fix the unexpected behaviour.  
**Important:**
This is only relevant during development, because usually AIIDA and EDDIE services are not deployed using the same host (localhost for the case of development).

### EDDIE Keycloak Theme

The current version of the EDDIE keycloak theme includes some very simple modifications only for the login page.
The source code and instructions can be found within the [keycloak eddie theme folder](../keycloak-eddie-theme).

## AIIDA Configuration

Several configurations can be applied through environment variables or the _application.properties_ file.
When using Docker, most of these properties should be configured in the [.env](docker/.env) file.

| Property                   | Description                                 |
|----------------------------|---------------------------------------------|
| SPRING_DATASOURCE_USERNAME | Username to authenticate to the TimescaleDB |
| SPRING_DATASOURCE_PASSWORD | Password to authenticate to the TimescaleDB |
| KEYCLOAK_HOST              | Host of the Keycloak instance               |
| KEYCLOAK_REALM             | Name of the Keycloak realm                  |
| KEYCLOAK_CLIENT            | Name of the Keycloak client                 |
| KEYCLOAK_CLIENT_SECRET     | The client secret for the Keycloak client   |

## API Documentation

OpenAPI documentation can be found here: http://localhost:8080/v3/api-docs

SwaggerUI is also included and can be found here: http://localhost:8080/swagger-ui/index.html

# Supported Smart Meters

Smart meters are the primary datasources and are gradually integrated in AIIDA. Data from all datasources is
automatically persisted in the TimescaleDB.

Currently, the following countries are supported:

| Country | Name of datasource                                                              |
|---------|---------------------------------------------------------------------------------|
| Austria | [OesterreichsEnergieAdapter](docs/datasources/at/OesterreichsEnergieAdapter.md) |
| France  | [MicroTeleinfoV3](docs/datasources/fr/MicroTeleinfoV3.md)                       |

![SupportedMeters](docs/Smart_Meter_supported_by_AIIDA.png)
*Map created with https://www.mapchart.net*
