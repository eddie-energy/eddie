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

| Configuration values                                         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.us.green.button.basepath`                  | Base path for the client (default is https://utilityapi.com/)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `region-connector.us.green.button.redirect.url`              | The redirect url for the OAuth flow                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `region-connector.us.green.button.client.api.token`          | The api token in order to check the API status                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `region-connector.us.green.button.webhook.secret`            | The webhook secret in order to validate webhook events. Not used yet, can contain any value.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `region-connector.us.green.button.client-ids.REPLACE_ME`     | The client ids of the utilities you want to support. The name of the utility should replace the `REPLACE_ME` placeholder.                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `region-connector.us.green.button.client-secrets.REPLACE_ME` | The client secrets of the utilities you want to support. The name of the utility should replace the `REPLACE_ME` placeholder.                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `region-connector.us.green-button.data-ready.polling`        | The frequency in which the region-connector checks if the data is available via the green button API. Uses Spring Cron syntax. Default is `0 0 * * * *`                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `region-connector.us.green-button.activation-batch-size`     | The batch size in which the collection of metering data should be triggered. The green button defers collecting data from the metered data administrator until it has been triggered. This means that no data is lost if the collection is triggered at a later point. If immediate activation is needed set to `1`. Recommended maximum is `1000` to avoid paging API calls. Keep in mind that if the collection is not finished before the data is polled incomplete or no data can be requested for the batch. Related to `region-connector.us.green-button.data-ready.polling`. |
| `region-connector.us.green.button.termination.retry`         | When termination of permission requests with the green button API should be retried. Uses Spring cron syntax. Default is hourly                                                                                                                                                                                                                                                                                                                                                                                                                                                     |

**IMPORTANT:** If the client IDs and secrets are to be configured via environment variables, leave the default configuration in the application.properties file.
Otherwise, spring is not able to pick up the overridden client IDs and secrets.
If the configuration is primarily done via `.properties` or `.yaml` files, this can be ignored, as spring can pick up the configuration from those.

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.us.green.button.basepath=https://utilityapi.com/
region-connector.us.green.button.redirect.url=https://your-eddie-instance.example/region-connectors/us-green-button/authorization-callback
region-connector.us.green.button.client.api.token=REPLACE_ME
region-connector.us.green.button.webhook.secret=REPLACE_ME
region-connector.us.green.button.client-ids.REPLACE_ME=REPLACE_ME
region-connector.us.green.button.client-secrets.REPLACE_ME=REPLACE_ME
region-connector.us.green-button.data-ready.polling=0 0 * * * *
region-connector.us.green-button.activation-batch-size=1
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_US_GREEN_BUTTON_BASEPATH=https://utilityapi.com/
REGION_CONNECTOR_US_GREEN_BUTTON_REDIRECT_URL=https://your-eddie-instance.example/region-connectors/us-green-button/authorization-callback
REGION_CONNECTOR_US_GREEN_BUTTON_CLIENT_API_TOKEN=REPLACE_ME
REGION_CONNECTOR_US_GREEN_BUTTON_WEBHOOK_SECRET=REPLACE_ME
REGION_CONNECTOR_US_GREEN_BUTTON_CLIENT_IDS_REPLACE_ME=REPLACE_ME
REGION_CONNECTOR_US_GREEN_BUTTON_CLIENT_SECRETS_REPLACE_ME=REPLACE_ME
REGION_CONNECTOR_US_GREEN_BUTTON_DATA_READY_POLLING=0 0 * * * *
REGION_CONNECTOR_US_GREEN_BUTTON_ACTIVATION_BATCH_SIZE=1
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
