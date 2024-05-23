# Region Connector for Spain (Datadis)

This README will guide you through the process of configuring a region connector for Spain.

## Prerequisites

- Register a user with Datadis [here](https://datadis.es/register).

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                           | Description                                                                                                                        |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.es.datadis.username`         | Username (DNI/NIF) used to log into the Datadis website private area.                                                              |
| `region-connector.es.datadis.password`         | Password for the user used to log into the Datadis website private area.                                                           |
| `region-connector.es.datadis.polling`          | Configures when future data should be polled. Uses Cron syntax. The default is 17 o'clock every day.                               |
| `region-connector.es.datadis.retry`            | Configures the interval in which failed permission requests should be retried. Uses Cron syntax. The default is hourly.            |
| `region-connector.es.datadis.timeout.schedule` | Configures the interval in which stale permission requests should be set to be timed out. Uses Cron syntax. The default is hourly. |
| `region-connector.es.datadis.timeout.duration` | Configures the duration after which a permission request is seen as stale. Uses hours as unit. The default is 24 hours.            |
| `region-connector.es.datadis.basepath`         | Changes the base path used by the datadis clients. The default is https://datadis.es/                                              |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.es.datadis.username=12345756X
region-connector.es.datadis.password=secret
region-connector.es.datadis.polling=0 0 17 * * *
region-connector.es.datadis.retry=0 0 * * * *
region-connector.es.datadis.timeout.schedule=0 0 * * * *
region-connector.es.datadis.timeout.duration=24
region-connector.es.datadis.basepath=https://datadis.es/
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_ES_DATADIS_USERNAME=12345756X
REGION_CONNECTOR_ES_DATADIS_PASSWORD=secret
REGION_CONNECTOR_ES_DATADIS_POLLING=0 0 17 * * *
REGION_CONNECTOR_ES_DATADIS_RETRY=0 0 * * * *
REGION_CONNECTOR_ES_DATADIS_TIMEOUT_SCHEDULE=0 0 * * * *
REGION_CONNECTOR_ES_DATADIS_TIMEOUT_DURATION=24
REGION_CONNECTOR_ES_DATADIS_BASEPATH=https://datadis.es/
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.