# Region Connector for the USA and Canada (Green Button Connect My Data)

This README will guide you through the process of configuring a region connector for the permission
administrators in Canada and the United States which supports the Green Button Connect My Data standard.
Green Button permission process is based on an [OAuth 2.0 Authorization Code
Grant](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1).
This region connector uses an event sourcing approach to create permission requests with the permission administrator.
Currently, it emits the following information:

- Connection Status Messages for changes in statuses of permission requests

## Prerequisites

- Register at [UtilityAPI](https://utilityapi.com/register)
- Register at the utility you want to support, those can be found [here](https://utilityapi.com/docs/utilities)
- After registration, all your utilities will be listed under _Utility Registration_ in
  your [settings](https://utilityapi.com/settings)
- For each registered utility, you will have OAuth settings in which you can find the following needed information for
  the configuration:
    - `Client ID`
    - `Client Secret`
    - `Redirect URI`, which you can add yourself
- At the section _API Settings_ you can generate an `API token` which is needed to check the status of the green button
  API
- Another section down you can find the Webhook Settings where you can add the path to the webhook of the EDDIE
  framework
    - You also are able to generate a `Webhook Secret` which is needed for the configuration
    - More on the webhook can be found under the section _Webhook_ or [here](https://utilityapi.com/docs/webhooks)

## Webhook

tbd

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                  | Description                                                                                                                                             |
|-------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.us.green.button.basepath`           | Base path for the client (default is https://utilityapi.com/DataCustodian/espi/1_1/resource)                                                            |
| `region-connector.us.green.button.redirect.url`       | The redirect url for the OAuth flow                                                                                                                     |
| `region-connector.us.green.button.client.api.token`   | The api token in order to check the API status                                                                                                          |
| `region-connector.us.green.button.webhook.secret`     | The webhook secret in order to validate webhook events                                                                                                  |
| `region-connector.us.green.button.client.ids`         | The client ids of the utilities you want to support                                                                                                     |
| `region-connector.us.green.button.client.secrets`     | The client secrets of the utilities you want to support                                                                                                 |
| `region-connector.us.green-button.data-ready.polling` | The frequency in which the region-connector checks if the data is available via the green button API. Uses Spring Cron syntax. Default is `0 0 * * * *` |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.us.green.button.basepath=https://utilityapi.com/DataCustodian/espi/1_1/resource
region-connector.us.green.button.redirect.url=https://your-eddie-instance.example/region-connectors/us-green-button/authorization-callback
region-connector.us.green.button.client.api.token=REPLACE_ME
region-connector.us.green.button.webhook.secret=REPLACE_ME
region-connector.us.green.button.client.ids={REPLACE: 'ME', REPLACE_ME: 'TOO'}
region-connector.us.green.button.client.secrets={REPLACE: 'ME', REPLACE_ME: 'TOO'}
region-connector.us.green-button.data-ready.polling=0 0 * * * *
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_US_GREEN_BUTTON_BASEPATH=https://utilityapi.com/DataCustodian/espi/1_1/resource
REGION_CONNECTOR_US_GREEN_BUTTON_REDIRECT_URL=https://your-eddie-instance.example/region-connectors/us-green-button/authorization-callback
REGION_CONNECTOR_US_GREEN_BUTTON_CLIENT_API_TOKEN=REPLACE_ME
REGION_CONNECTOR_US_GREEN_BUTTON_WEBHOOK_SECRET=REPLACE_ME
REGION_CONNECTOR_US_GREEN_BUTTON_CLIENT_IDS={REPLACE: 'ME', REPLACE_ME: 'TOO'}
REGION_CONNECTOR_US_GREEN_BUTTON_CLIENT_SECRETS={REPLACE: 'ME', REPLACE_ME: 'TOO'}
REGION_CONNECTOR_US_GREEN_BUTTON_DATA_READY_POLLING=0 0 * * * *
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
