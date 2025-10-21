# AIIDA Operation Manual

This document provides instructions on how to operate and manage the Administrative Interface for In-House Data Access (AIIDA).

## Quickstart

A quickstart configuration to run AIIDA is provided in the
`aiida/docker` folder of the development project or under this [link](https://github.com/eddie-energy/eddie/tree/main/aiida/docker).
This setup uses Docker Compose to orchestrate the necessary services. To get started, follow these steps:

1. Download the quickstart configuration from the provided [aiida/docker](https://github.com/eddie-energy/eddie/tree/main/aiida/docker)
2. Run `docker compose --profile dockerized-aiida up -d` in that folder.
3. Open the browser on `http://localhost:8080` to access the AIIDA web interface.
4. You can log in with the default credentials:
    - Username: `aiida`
    - Password: `aiida`
5. For adding data sources, please refer to the [data source section](data-sources/data-sources.md).
6. For adding permissions, please refer to the [permission section](permission/permission.md).

## Installation

The
`aiida/docker` folder contains a container configuration inside the [compose file](https://github.com/eddie-energy/eddie/tree/main/aiida/docker/compose.yml).

### Additional Services

AIIDA requires additional services to function properly. The following services are included in the Docker Compose setup:

| **Service**     | **Description**                                                                                                                    |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------|
| **TimescaleDB** | A time-series database built on PostgreSQL. It is used to store information about permissions, data sources, and time-series data. |
| **EMQX**        | An open-source MQTT broker that facilitates communication between data sources and AIIDA.                                          |
| **Keycloak**    | An open-source identity and access management solution. It is used for authentication and authorization.                           |

A sample configuration for these services is also provided in AIIDA's [compose file](https://github.com/eddie-energy/eddie/tree/main/aiida/docker/compose.yml).

## Configuration

It is recommended to configure AIIDA using the .env file provided in the `aiida/docker` folder in combination with the
`compose.yml` file.

| Parameter                                              | Description                                                                          |
|--------------------------------------------------------|--------------------------------------------------------------------------------------|
| MQTT_INTERNAL_HOST                                     | The hostname for docker internal communication                                       |
| MQTT_EXTERNAL_HOST                                     | The hostname for external communication                                              |
| MQTT_BCRYPT_SALT_ROUNDS                                | The bcrypt salt rounds for hashing passwords                                         |
| MQTT_PASSWORD                                          | The password for the MQTT user `aiida`                                               |
| AIIDA_EXTERNAL_HOST                                    | The hostname for accessing the AIIDA web UI                                          |
| SPRING_DATASOURCE_HOST                                 | The hostname of the TimescaleDB service                                              |
| SPRING_DATASOURCE_PORT                                 | The port of the TimescaleDB service                                                  |
| SPRING_DATASOURCE_DATABASE                             | The database name for AIIDA in TimescaleDB                                           |
| SPRING_DATASOURCE_USERNAME                             | The username for accessing the database                                              |
| SPRING_DATASOURCE_PASSWORD                             | The password for accessing the database                                              |
| EMQX_DATABASE_PASSWORD                                 | The password for the `emqx` user in TimescaleDB                                      |
| KEYCLOAK_ADMIN_USERNAME                                | The admin username for Keycloak                                                      |
| KEYCLOAK_ADMIN_PASSWORD                                | The admin password for Keycloak                                                      |
| KEYCLOAK_EXTERNAL_HOST                                 | The hostname for accessing Keycloak                                                  |
| KEYCLOAK_INTERNAL_HOST                                 | The hostname for docker internal communication                                       |
| AIIDA_CLEANUP_CLEANUP_INTERVAL                         | Specifies in which fixed duration the cleanup task is scheduled (Default: Every 24h) |
| AIIDA_CLEANUP_ENTITIES_AIIDA_RECORD_RETENTION          | Specifies the time-to-live for an AIIDA_RECORD                                       |
| AIIDA_CLEANUP_ENTITIES_FAILED_TO_SEND_ENTITY_RETENTION | Specifies the time-to-live for a FAILED_TO_SEND_ENTITY                               |
| AIIDA_CLEANUP_ENTITIES_INBOUND_RECORD_RETENTION        | Specifies the time-to-live for an INBOUND_RECORD                                     |

## Application Information

After the first startup of AIIDA, an application UUID is generated and stored in the database.
This UUID can be found in the `aiida_application_information` table in the [database](database/database.md).
It is used by the EDDIE instance to differentiate between multiple AIIDA instances.

## Internal APIs

AIIDA provides internal APIs for additional information about itself.

### Actuator API

AIIDA uses Spring to expose health information for data sources.
The actuator API is available at `<AIIDA_EXTERNAL_HOST>/actuator` and the health endpoint at
`<AIIDA_EXTERNAL_HOST>/actuator/health`.

### AIIDA Record API

TBD

### Inbound API

Via the [Inbound Data Source](data-sources/mqtt/inbound/inbound-data-source.md) the EP can send data to AIIDA via MQTT.
For that purpose, the user must accept an inbound permission, which automatically creates an inbound data source.

The latest retrieved inbound data can be accessed via the Inbound API.
See [Inbound Data Source](data-sources/mqtt/inbound/inbound-data-source.md#accessing-inbound-data) for more information.