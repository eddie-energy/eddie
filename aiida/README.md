# AIIDA - Administrative Interface for In-house Data Access

Development & Deployment Strategy can be
found [here](https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy).

## How to run

Use the following gradle task to run this spring boot application.
Configuration via the [application.properties](src/main/resources/application.yml) file is necessary.
Additionally, a [TimescaleDB](https://www.timescale.com/) has to be started manually.

- `./gradlew bootRun`

The permissions REST API will be exposed on the [default Spring Boot port (localhost:8080)](http://localhost:8080)

### Docker

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
To overcome this issue, the property `server.servlet.session.cookie.name` can e.g. be set to `AIIDA_SESSION_ID`, which will fix the unexpected behaviour.  
***Important:*** This is only relevant during development, because usually AIIDA and EDDIE services are not deployed using the same host (localhost for the case of development).

## AIIDA configuration

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

## API documentation

OpenAPI documentation can be found here: http://localhost:8080/v3/api-docs

SwaggerUI is also included and can be found here: http://localhost:8080/swagger-ui/index.html

# Supported datasources

Smart meters are the primary datasources and are gradually integrated in AIIDA. Data from all datasources is
automatically persisted in the TimescaleDB.

Currently, the following countries are supported:

| Country | Name of datasource                                                              |
|---------|---------------------------------------------------------------------------------|
| Austria | [OesterreichsEnergieAdapter](docs/datasources/at/OesterreichsEnergieAdapter.md) |

![SupportedMeters](docs/Smart_Meter_supported_by_AIIDA.png)
*Map created with https://www.mapchart.net*
