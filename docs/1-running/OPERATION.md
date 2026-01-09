# EDDIE Framework operation manual

## Quickstart

A quickstart configuration to run the EDDIE Framework is provided in the `env` folder of the development project.
The steps to make it run are:

1. Download the quickstart configuration folder from [EDDIE /env](https://github.com/eddie-energy/eddie/tree/main/env)
2. Run `docker compose up -d` in that folder.
3. Open browser on http://localhost:9000/prototype/main/ and experiment with the provided sample application.
4. As many permission administrators and metered data administrators require additional installation or registration
   steps, it's best to try out the framework functionality using the permission administrator called _simulation_.

## Installation

TBD.

A sample container configuration in `docker-compose.yml`:

```yaml
version: "3.9"
services:
  eddie:
    image: ghcr.io/eddie-energy/eddie:latest
    environment:
      JDBC_URL: "jdbc:postgresql://localhost:5432/example_app"
      JDBC_USER: "test"
      JDBC_PASSWORD: "test"
      PUBLIC_CONTEXT_PATH: "" # default value
      EDDIE_DATA_NEEDS_CONFIG_FILE: "./config/data-needs.json" # default value
    volumes:
      - ./ponton:/ponton
      - ./data-needs.json:/opt/eddie/config/data-needs.json
```

| Variable                       | Description                             |
|--------------------------------|-----------------------------------------|
| `EDDIE_DATA_NEEDS_CONFIG_FILE` | File containing data needs definitions. |

As the configuration of region connectors is quite complex and there are many properties, the environment is
configured in the accompanying
`.env` file (see [EDDIE /env](https://github.com/eddie-energy/eddie/tree/main/env) directory for a reference).
The example `.env` file contains all configuration options.

## Update

TBD.

## Using the EDDIE Button in an application

To use the _EDDIE Button_ in an eligible party application, the EP application has to include the button in its HTML
page. It is implemented as a standard HTML custom element and can be loaded from the EDDIE instance.

```html

<script
  type="module"
  src="${eddieUrl}/lib/eddie-components.js"></script>
<!-- ... -->
<eddie-connect-button
  connection-id="1"
  data-need-id="9bd0668f-cc19-40a8-99db-dc2cb2802b17"
></eddie-connect-button>
```

`${eddieUrl}` is to be replaced with the public base URL of your EDDIE instance.

For more information see [Embed the EDDIE Button in your application](eddie-button/eddie-button.md).

## Configuration

It is recommended to configure EDDIE core and the region connectors via the `.env` file in combination with the `docker-compose.yml` file.

### Configuring EDDIE Core

EDDIE Core can be configured by the following environment variables.
You can also modify the [application.properties](https://github.com/eddie-energy/eddie/blob/main/core/src/main/resources/application.properties) file directly, but the recommendation is to use the `.env` which accompanies the [docker-compose.yml](https://github.com/eddie-energy/eddie/blob/main/env/docker-compose.yml) file.

| Parameter                                                            | Description                                                                                                                                                                                                                                                                                                    |
|----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CORE_PORT`                                                          | Port on which the server should listen. 8080 by default.                                                                                                                                                                                                                                                       |
| `JDBC_URL`                                                           | JDB URL to PostgreSQL database where EDDIE will store permission requests and data needs.                                                                                                                                                                                                                      |
| `JDBC_USER`                                                          | Username to authenticate with the PostgreSQL server.                                                                                                                                                                                                                                                           |
| `JDBC_PASSWORD`                                                      | Password to authenticate with the PostgreSQL server.                                                                                                                                                                                                                                                           |
| `EDDIE_CORS_ALLOWED_ORIGINS`                                         | Pattern for allowed CORS origins. See [SpringDoc](<https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/CorsRegistration.html#allowedOriginPatterns(java.lang.String...)>) for more details. If not specified, no CORS requests will be allowed. |
| `EDDIE_JWT_HMAC_SECRET`                                              | Secret used to sign JWTs issued by EDDIE. Supply 32 random (!) bytes encoded as Base64 string. Can be generated on linux via `openssl rand -base64 32`.                                                                                                                                                        |
| `EDDIE_PUBLIC_URL`                                                   | Public URL where external clients can reach EDDIE core.                                                                                                                                                                                                                                                        |
| `EDDIE_MANAGEMENT_URL`                                               | Full URL of the management APIs and outbound connectors. Usually the combination of `EDDIE_PUBLIC_URL` and `EDDIE_MANAGEMENT_SERVER_PORT`.                                                                                                                                                                     |
| `KAFKA_ENABLED`                                                      | Set to `true` to enable publishing of status & data messages to the specified Kafka cluster.                                                                                                                                                                                                                   |
| `KAFKA_BOOTSTRAP_SERVERS`                                            | Comma separated list of Kafka server IPs/hostnames.                                                                                                                                                                                                                                                            |
| `EDDIE_RAW_DATA_OUTPUT_ENABLED`                                      | If set to `true`, supporting region connectors will publish the raw message as they receive it from the MDA to a dedicated Kafka topic.                                                                                                                                                                        |
| `EDDIE_PERMISSION_REQUEST_TIMEOUT_DURATION`                          | Sets the duration after which a permission request, which was neither accepted nor rejected, is considered stale. Default is 168 hours (7 days).                                                                                                                                                               |
| `EDDIE_PERMISSION_REQUEST_TIMEOUT_SCHEDULE`                          | Sets the schedule when stale permission requests should be timed out. Uses Spring Cron syntax. Default is hourly.                                                                                                                                                                                              |
| `EDDIE_MANAGEMENT_SERVER_PORT`                                       | Port for the management api and outbound connectors.                                                                                                                                                                                                                                                           |
| `EDDIE_MANAGEMENT_SERVER_URLPREFIX`                                  | Url prefix for the management api (must not be used for other purposes).                                                                                                                                                                                                                                       |
| `EDDIE_DATA_NEEDS_CONFIG_DATA_NEED_SOURCE`                           | Source where to read data needs from. Either `config` or `database`.                                                                                                                                                                                                                                           |
| `EDDIE_DATA_NEEDS_CONFIG_FILE`                                       | File containing data needs definitions.                                                                                                                                                                                                                                                                        |
| `EDDIE_CONVERTERS_POWER`                                             | When set to `true` enables the converter to convert energy to power measurements.                                                                                                                                                                                                                              |
| `EDDIE_CONVERTERS_ENERGY`                                            | When set to `true` enables the converter to convert power to energy measurements.                                                                                                                                                                                                                              |
| `REGION_CONNECTOR_<country-code>_<permission-administrator>_ENABLED` | `true` to enable the specific region connector. By default, only region connectors requiring no explicit configuration are enabled. Ensure that you set any other required configuration for the region connector as otherwise EDDIE may fail to start.                                                        |
| `SPRING_CODEC_MAX_IN_MEMORY_SIZE`                                    | Sets the maximum size that is used for webclients and webserver. Recommended is at least `20MB`, which can be increased, if the `DataBufferLimitException` occurs.                                                                                                                                             |
| `SPRING_PROFILES_ACTIVE`                                             | Setting the spring profile to `dev` will use defaults for some configuration properties. See the [`application-dev.properties`](https://github.com/eddie-energy/eddie/blob/main/core/src/main/resources/application-dev.properties) for more information.                                                      |
| `EDDIE_DEMO_BUTTON_ENABLED`                                          | Provides a simple demo page to test the EDDIE button with different configurations under the `/demo` path.                                                                                                                                                                                                     |

### Configuring region connectors

To retrieve data from various regions and countries, it is crucial to correctly set up the relevant region connectors.

For each region connector, specific configurations and prerequisites are necessary for operation.
Details for these setups are provided in the README file of the individual region connector. You can locate these files
under `region-connectors/region-connector-<country-code>-<permission-administrator>.md`.

Or you can use the following links:

- [AIIDA (Near real-time data)](region-connectors/region-connector-aiida.md)
- [Austria (EDA)](region-connectors/region-connector-at-eda.md)
- [Belgium (Fluvius)](region-connectors/region-connector-be-fluvius.md)
- [Denmark (Energinet)](region-connectors/region-connector-dk-energinet.md)
- [Finland (Fingrid)](region-connectors/region-connector-fi-fingrid.md)
- [France (Enedis)](region-connectors/region-connector-fr-enedis.md)
- [Netherlands (Mijn Aansluiting)](region-connectors/region-connector-nl-mijn-aansluiting.md)
- [Spain (Datadis)](region-connectors/region-connector-es-datadis.md)
- [US/Canada (Green Button)](region-connectors/region-connector-us-green-button.md)

### Configuring outbound connectors

There are three outbound connectors, which can be used by the eligible party to interact with eddie.
If the outbound connector provides a web interface, it will be available via the configured `eddie.management.server.port`.

- [Kafka outbound connector](outbound-connectors/outbound-connector-kafka.md).
- [Admin Console](admin-console.md)
- [AMQP outbound connector](outbound-connectors/outbound-connector-amqp.md)

### Business domain related configuration

#### Common Information Model (CIM)

For the mapping of region specific data to the common information model (CIM) the following configuration parameters
need to be set:

| Parameter                                 | Type                               | Description                                                                                         |
|-------------------------------------------|------------------------------------|-----------------------------------------------------------------------------------------------------|
| cim.eligible-party.national-coding-scheme | A valid CodingSchemeTypeList value | Most of the time just 'N' + your country code e.g NAT if you are located in Austria                 |
| cim.eligible-party.fallback.id            | String                             | Fallback ID for the eligible party in case the region does not provide an ID for the eligible party |

E.g. eligible party in Austria:

```
cim.eligible-party.national-coding-scheme=NAT
cim.eligible-party.fallback.id=EDDIE-Online
```

#### Data need configuration

A data need describes a configuration for the _Connect Button_.
By using that button, the type of data and time frame is predefined so that the EP application receives data that it actually needs to perform its job.

Data needs can be configured in two ways: via a JSON file that is read on startup, they can be created via a REST-ful API which stores the data needs in the core's database.

| Parameter                                | Type              | Description                                            |
|------------------------------------------|-------------------|--------------------------------------------------------|
| eddie.data-needs-config.data-need-source | CONFIG / DATABASE | Specifies the location where data needs are read from. |

If this is set to `CONFIG`, the property `EDDIE_DATA_NEEDS_CONFIG_FILE` needs to be set, otherwise the file is ignored.
It is not possible to combine `CONFIG` and `DATABASE` modes.
For more information see [data-needs](./../2-integrating/data-needs.md).

### Configuring the example app

If you are using the example app and you change configuration parameters for the _core_, you might need to update its configuration as well.
Please refer to the [readme.md](example-app.md) of the example app.

## Internal APIs

EDDIE provides internal APIs for additional information about it.
For usages for the endpoints see [api.http](https://github.com/eddie-energy/eddie/blob/main/core/api.http).

### Actuator API

EDDIE uses Spring actuator to expose health information for each region connector.
The actuator API is available at `<host>:<port>/actuator` and the health endpoint at `<host>:<port>/actuator/health`.

### Information about region connectors

There are multiple APIs to gather information about region connectors.

#### Supported features

The endpoint for supported features is available under `<host>:<port>/<eddie.management.server.urlprefix>/region-connectors/supported-features`.

#### Supported Data Needs

The endpoint to query the supported data needs per region connector is available under `<host>:<port>/<eddie.management.server.urlprefix>/region-connectors/supported-data-needs`.

Each region connector declares which data need types are supported in what configuration.
This API returns these rules for each region connector.
