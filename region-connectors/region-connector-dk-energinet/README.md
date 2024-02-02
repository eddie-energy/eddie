# Region Connector for Denmark (Energinet)

This README will guide you through the process of configuring a region connector for Denmark.

## Prerequisites

- You have to have an account at the [Eloverblik](https://eloverblik.dk/customer/login) web portal.
- In this web portal you have to generate an API key, also called refreshToken in this region-connector's context,
  which you have to provide when sending a request.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, right now only the URL to the
API endpoints are needed and already provided. Right now the customer API endpoint is the only one in use.

| Configuration values                                     | Description                                                                                          |
|----------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `region-connector.dk.energinet.customer.client.basepath` | The URL to the customer API endpoint.                                                                |
| `region-connector.dk.energinet.customer.id`              | A unique identifier for the eligible party, should not be changed.                                   |
| `region-connector.dk.energinet.polling`                  | Configures when future data should be polled. Uses Cron syntax. The default is 17 o'clock every day. |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.dk.energinet.customer.client.basepath=https://api.eloverblik.dk/customerapi
region-connector.dk.energinet.customer.id=my-unique-id
region-connector.dk.energinet.polling=0 0 17 * * *
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_DK_ENERGINET_CUSTOMER_CLIENT_BASEPATH=https://api.eloverblik.dk/customerapi
REGION_CONNECTOR_DK_ENERGINET_CUSTOMER_ID=my-unique-id
REGION_CONNECTOR_DK_ENERGINET_POLLING=0 0 17 * * *
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.