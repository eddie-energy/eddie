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
6. For adding permissions, please refer to the [permission section](permission.md).

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

| Parameter                                           | Description                                                                                                                          |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| AIIDA_EXTERNAL_HOST                                 | Network-accessible host of the AIIDA instance (defaults to http://localhost:8080)                                                    |
| AIIDA_CORS_ALLOWED_ORIGINS                          | The origins that are allowed to communicate with AIIDA (see [reverse proxy deployments](#reverse-proxy-deployment))                  |
| AIIDA_KEYCLOAK_ACCOUNT_URI                          | Specifies the URI to which users are redirected for account settings. By default, this points to Keycloak's account management page. |
| AIIDA_CLEANUP_INTERVAL                              | Specifies in which fixed duration the cleanup task is scheduled (default: P1D)                                                       |
| AIIDA_CLEANUP_ENTITIES_AIIDARECORD_RETENTION        | Specifies the time-to-live for an AIIDA_RECORD (default: P1D)                                                                        |
| AIIDA_CLEANUP_ENTITIES_FAILEDTOSENDENTITY_RETENTION | Specifies the time-to-live for a FAILED_TO_SEND_ENTITY (default: P1D)                                                                |
| AIIDA_CLEANUP_ENTITIES_INBOUNDRECORD_RETENTION      | Specifies the time-to-live for an INBOUND_RECORD (default: P1D)                                                                      |
| SPRING_DATASOURCE_HOST                              | The hostname of the TimescaleDB service                                                                                              |
| SPRING_DATASOURCE_PORT                              | The port of the TimescaleDB service                                                                                                  |
| SPRING_DATASOURCE_DATABASE                          | The database name for AIIDA in TimescaleDB                                                                                           |
| SPRING_DATASOURCE_USERNAME                          | The username for accessing the database                                                                                              |
| SPRING_DATASOURCE_PASSWORD                          | The password for accessing the database                                                                                              |
| MQTT_INTERNAL_HOST                                  | The hostname for docker internal communication                                                                                       |
| MQTT_EXTERNAL_HOST                                  | The hostname for external communication                                                                                              |
| MQTT_BCRYPT_SALT_ROUNDS                             | The bcrypt salt rounds for hashing passwords                                                                                         |
| MQTT_PASSWORD                                       | The password for the MQTT user `aiida`                                                                                               |
| MQTT_TLS_CERTIFICATE_PATH                           | Filepath of TLS certificate for MQTT broker (can be mounted to Docker container)                                                     |
| EMQX_DATABASE_PASSWORD                              | The password for the `emqx` user in TimescaleDB                                                                                      |
| KEYCLOAK_ADMIN_USERNAME                             | The admin username for Keycloak                                                                                                      |
| KEYCLOAK_ADMIN_PASSWORD                             | The admin password for Keycloak                                                                                                      |
| KEYCLOAK_EXTERNAL_HOST                              | The hostname for accessing Keycloak                                                                                                  |
| KEYCLOAK_INTERNAL_HOST                              | The hostname for docker internal communication                                                                                       |
| KEYCLOAK_REALM                                      | The Keycloak realm used for AIIDA                                                                                                    |
| KEYCLOAK_CLIENT ID                                  | The Keycloak client ID used for AIIDA                                                                                                |
| KEYCLOAK_CLIENT_SECRET                              | The Keycloak client secret used for AIIDA                                                                                            |

### Reverse Proxy Deployment

If you are running an AIIDA instance behind a reverse proxy (e.g. nginx) to make it accessible everywhere, it is
necessary to add the origin of the AIIDA instance to the allowed origins.
This can be done by setting the config `aiida.cors.allowed-origins` or using the
`AIIDA_CORS_ALLOWED_ORIGINS` environment variable.
For example, if your AIIDA instance is reachable at the url `https://aiida.eddie.energy` you have to set the value of
`AIIDA_CORS_ALLOWED_ORIGINS` to `https://aiida.eddie.energy`.
To the best of our knowledge, this is only necessary for reverse proxy deployments and not e.g. using Kubernetes.

### Customizing Brand Icons in the UI

The AIIDA brand icons can be customized by replacing the SVG files inside `aiida/docker/svgs` with your custom icons.
For best results try to use SVGs with a similar aspect ratio to the original icons. If the icons are deleted or otherwise invalid, fallback icons will be used.

- `HeaderLogo.svg` the logo used in the header.
- `BackgroundBrandmark.svg` the icon that is displayed in the background.
- `favicon.svg` the favicon

## Application Information

After the first startup of AIIDA, an application UUID is generated and stored in the database.
This UUID can be found in the `aiida_application_information` table in the [database](database.md).
It is used by the EDDIE instance to differentiate between multiple AIIDA instances.

## Internal APIs

AIIDA provides internal APIs for additional information about itself.

### OpenAPI Documentation

OpenAPI documentation can be found here: `<AIIDA_EXTERNAL_HOST>/v3/api-docs`

SwaggerUI is also included and can be found here: `<AIIDA_EXTERNAL_HOST>/swagger-ui/index.html`

### Actuator API

AIIDA uses Spring to expose health information for data sources.
The actuator API is available at `<AIIDA_EXTERNAL_HOST>/actuator` and the health endpoint at
`<AIIDA_EXTERNAL_HOST>/actuator/health`.

### Record API

The **Record API** provides access to the latest record (message) that AIIDA has sent or received.

- **Permission Record:**  
  Returns the most recent message associated with a specific permission.  
  For **outbound permissions**, this includes the fields `topic`, `serverUri`, `timestamp`, `schema`, and `payload`.  
  For **inbound permissions**, see the
  [Inbound Data Source](data-sources/mqtt/inbound/inbound-data-source.md#accessing-inbound-data) section for details.

- **Data Source Record:**  
  Returns the most recent message received from a specific data source, converted into the [Raw Message](schemas/raw/raw.md) format.

In the AIIDA web interface, the corresponding record can be downloaded directly from the **data source** or **permission
detail** views.  
All records are provided in JSON format to simplify inspection and debugging.

Use this API to monitor and verify the latest communication between AIIDA and connected data sources or permissions.

### Inbound API

Via the [Inbound Data Source](data-sources/mqtt/inbound/inbound-data-source.md) the EP can send data to AIIDA via MQTT.
For that purpose, the user must accept an inbound permission, which automatically creates an inbound data source.

The latest retrieved inbound data can be accessed via the Inbound API.
See [Inbound Data Source](data-sources/mqtt/inbound/inbound-data-source.md#accessing-inbound-data) for more information.

## Helm Chart

> [!WARNING]
> This Helm chart is not actively maintained and may not be up to date with the latest AIIDA features or deployment best practices.

The AIIDA Helm chart is available in the [dedicated repository](https://github.com/eddie-energy/aiida-helm).
It enables the deployment of AIIDA on Kubernetes clusters using Helm.

For more details, refer to the [AIIDA Helm Chart repository](https://github.com/eddie-energy/aiida-helm).