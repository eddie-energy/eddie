# ![AIIDA - Administrative Interface for In-house Data Access](docs/images/aiida-horizontal.svg)

The Administrative Interface for In-house Data Access (AIIDA) 
connects to various metering devices such as smart meters and home automation systems,
to stream near real-time energy data and other data to consumers like the EDDIE Framework.

To learn more about the architecture of AIIDA, 
you can visit its [architecture documentation](https://eddie-web.projekte.fh-hagenberg.at/architecture/aiida/aiida.html).
If you want to contribute to this repository, please take a look at our [contributing guide](../CONTRIBUTING.md).

## Prerequisites

In order for AIIDA to run, it is necessary to start a [TimescaleDB](https://www.timescale.com/) and a [Keycloak](https://www.keycloak.org/) instance. The predefined docker-compose.yml for starting
those services can be found in the [docker](docker) folder.
To use data sources that use MQTT, an EMQX instance is required additionally, which is also included in the docker-compose.yml.
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

### Keycloak Integration in Docker Network

To enable Keycloak usage within a Docker network, several configurations must be made in the `application.yml` file of the Spring application.
When setting the property `issuer-uri` in the `application.yml`, the application retrieves the URIs from `http://localhost:8888/realms/EDDIE/.well-known/openid-configuration`.
Since this URI is not accessible from the Docker network, the required URIs must be defined explicitly.

The following properties must be set in the `application.yml` file:

| Property            | Description                                                   |
|---------------------|---------------------------------------------------------------|
| authorization-uri   | URI for redirecting users for authorization.                  |
| token-uri           | URI to exchange the access code for an access token.          |
| user-info-uri       | URI to fetch user information.                                |
| jwk-set-uri         | URI to obtain the public key for verifying JWTs.              |
| user-name-attribute | JWT claim that contains the username.                         |
| redirect-uri        | URI for redirecting users back to the application post-login. |
| end-session-uri     | URI for logging out of Keycloak sessions.                     |

Additionally, Keycloak requires a configured frontend URL to validate the issuer URI. This is specified using the `KC_HOSTNAME` variable in the `compose.yml` file.
The provided `compose.yml` file provides a preconfiguration of these values for keycloak, you can configure it using the environments:

- `AIIDA_EXTERNAL_HOST`
- `KEYCLOAK_INTERNAL_HOST`
- `KEYCLOAK_EXTERNAL_HOST`

For a local development setup these values can be configured as follows (defaults of `.env` file):

- `AIIDA_EXTERNAL_HOST=http://localhost:8080`
- `KEYCLOAK_INTERNAL_HOST=http://keycloak:8080`
- `KEYCLOAK_EXTERNAL_HOST=http://localhost:8888`

For a production deployment setup these values can be configured as follows assuming keycloak is running on `keycloak.eddie.energy` and aiida is running on `aiida.eddie.energy`:

- `AIIDA_EXTERNAL_HOST=https://aiida.eddie.energy`
- `KEYCLOAK_INTERNAL_HOST=https://keycloak.eddie.energy`
- `KEYCLOAK_EXTERNAL_HOST=https://keycloak.eddie.energy`

### EDDIE Keycloak Theme

The current version of the EDDIE keycloak theme includes some very simple modifications only for the login page.
The source code and instructions can be found within the [keycloak eddie theme folder](../keycloak-eddie-theme).

## AIIDA Configuration

Several configurations can be applied through environment variables or the _application.properties_ file.
When using Docker, most of these properties should be configured in the [.env](docker/.env) file.

| Property                   | Description                                                                                                                          |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| AIIDA_EXTERNAL_HOST        | Network-accessible host of the AIIDA instance                                                                                        |
| AIIDA_CORS_ALLOWED_ORIGINS | The origins that are allowed to communicate with AIIDA (necessary for deployments with reverse proxies)                              |
| AIIDA_KEYCLOAK_ACCOUNT_URI | Specifies the URI to which users are redirected for account settings. By default, this points to Keycloak's account management page. |
| SPRING_DATASOURCE_USERNAME | Username to authenticate to the TimescaleDB                                                                                          |
| SPRING_DATASOURCE_PASSWORD | Password to authenticate to the TimescaleDB                                                                                          |
| KEYCLOAK_INTERNAL_HOST     | Internal network host of the Keycloak instance (e.g. inside Docker network)                                                          |
| KEYCLOAK_EXTERNAL_HOST     | Network-accessible host of the Keycloak instance                                                                                     |
| KEYCLOAK_REALM             | Name of the Keycloak realm                                                                                                           |
| KEYCLOAK_CLIENT            | Name of the Keycloak client                                                                                                          |
| KEYCLOAK_CLIENT_SECRET     | The client secret for the Keycloak client                                                                                            |
| KEYCLOAK_ADMIN_USERNAME    | Username of the Keycloak admin                                                                                                       |
| KEYCLOAK_ADMIN_PASSWORD    | Password of the Keycloak admin                                                                                                       |
| MQTT_USERNAME              | Username of MQTT broker                                                                                                              |
| MQTT_PASSWORD              | Password of MQTT broker                                                                                                              |
| MQTT_EXTERNAL_HOST         | Network-accessible host of the MQTT broker                                                                                           |
| MQTT_INTERNAL_HOST         | Internal network host of the MQTT broker (e.g. inside Docker network)                                                                |
| MQTT_BCRYPT_SALT_ROUNDS    | Number of rounds for bcrypt hashing of MQTT passwords (default: 12)                                                                  |

### Reverse Proxy Deployment

If you are running an AIIDA instance behind a reverse proxy (e.g. nginx) to make it accessible everywhere, it is necessary to add the origin of the AIIDA instance to the allowed origins.
This can be done by setting the config `aiida.cors.allowed-origins` or using the `AIIDA_CORS_ALLOWED_ORIGINS` environment variable.
For example if your AIIDA instance is reachable at the url `https://aiida.eddie.energy` you have to set the value of `AIIDA_CORS_ALLOWED_ORIGINS` to `https://aiida.eddie.energy`.
To the best of our knowledge this is only necessary for reverse proxy deployments and not e.g. using Kubernetes.

## API Documentation

OpenAPI documentation can be found here: http://localhost:8080/v3/api-docs

SwaggerUI is also included and can be found here: http://localhost:8080/swagger-ui/index.html

## Supported Smart Meters

Smart meters are the primary datasources and are gradually integrated in AIIDA. Data from all datasources is
automatically persisted in the TimescaleDB.

Currently, the following countries are supported:

| Country                        | Name of datasource                                                              |
| ------------------------------ | ------------------------------------------------------------------------------- |
| AT                             | [OesterreichsEnergieAdapter](docs/datasources/at/OesterreichsEnergieAdapter.md) |
| FR                             | [MicroTeleinfoV3](docs/datasources/fr/MicroTeleinfoV3.md)                       |
| NL, BE, SE, DK, FI, HU, LT, CH | [SmartGatewaysAdapter](docs/datasources/sga/SmartGatewaysAdapter.md)            |

![SupportedMeters](docs/images/Smart_Meter_supported_by_AIIDA.png)
_Map created with https://www.mapchart.net_

## Modbus Configuration Documentation

For details on configuring Modbus-based smart meters and other energy devices within AIIDA, including virtual datapoints, endian handling, and transformation logic, please refer to the full documentation:

📄 [Modbus Configuration Documentation](docs/modbus/ModbusConfigDocumentation.md)