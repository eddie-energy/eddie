# Region Connector for Belgium (Fluvius)

This README will guide you through the process of configuring a region connector for flemish Belgium.

## Prerequisites

- Register as a partner with [Fluvius](https://partner.fluvius.be/nl/energiedienstverleners), a belgian company ID is required for this.
- During the onboarding process, a certificate has to be provided, which fluvius uses to allowlist your application.
- Furthermore, credentials are provided to access the API.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                 | Description                                                                                                                                                      |
|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.be.fluvius.oauth.token-url`        | The URL to obtain an API token, again provided by fluvius during the onboarding.                                                                                 |
| `region-connector.be.fluvius.oauth.client-id`        | Client ID to obtain an API token, provided by Fluvius.                                                                                                           |
| `region-connector.be.fluvius.oauth.client-secret`    | Client secret to obtain an API token, provided by Fluvius.                                                                                                       |
| `region-connector.be.fluvius.oauth.tenant-id`        | The tenant ID identifies the microsoftonline OAuth server.                                                                                                       |
| `region-connector.be.fluvius.oauth.scope`            | Scope of the requested API token, provided by Fluvius.                                                                                                           |
| `region-connector.be.fluvius.base-url`               | The base-url of the API, the urls are provided during the onboarding process by Fluvius.                                                                         |
| `region-connector.be.fluvius.subscription-key`       | The subscription key to access the API, provided by Fluvius.                                                                                                     |
| `region-connector.be.fluvius.contract-number`        | Contract number to create permission requests via Fluvius' API, provided by Fluvius.                                                                             |
| `region-connector.be.fluvius.mock-mandates`          | Is either `true` or `false`. Set to `true` if the sandbox environment provided by Fluvius is used. Can be used to test the region-connector against the sandbox. |
| `region-connector.be.fluvius.retry`                  | Configures when a failed to send permission request should be retried. Uses Spring Cron syntax. Default is `0 0 * * * *`.                                        |
| `region-connector.be.fluvius.check-acceptance`       | Configures when the region-connector should check for acceptance of permission requests. Uses Spring Cron syntax. Default is `0 0 * * * *`.                      |
| `spring.ssl.bundle.pem.fluvius.keystore.certificate` | Specify the public part of the certificate that was allowlisted by Fluvius.                                                                                      |
| `spring.ssl.bundle.pem.fluvius.keystore.private-key` | Specify the private part of the certificate that was allowlisted by Fluvius.                                                                                     |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.be.fluvius.base-url=REPLACE_ME
region-connector.be.fluvius.oauth.token-url=https://login.microsoftonline.com/${region-connector.be.fluvius.oauth.tenant-id}/oauth2/V2.0/token
region-connector.be.fluvius.oauth.client-id=REPLACE_ME
region-connector.be.fluvius.oauth.tenant-id=REPLACE_ME
region-connector.be.fluvius.oauth.client-secret=REPLACE_ME
region-connector.be.fluvius.oauth.scope=REPLACE_ME
region-connector.be.fluvius.subscription-key=REPLACE_ME
region-connector.be.fluvius.contract-number=REPLACE_ME
region-connector.be.fluvius.mock-mandates=false
region-connector.be.fluvius.retry=0 0 * * * *
region-connector.be.fluvius.check-acceptance=0 0 * * * *
spring.ssl.bundle.pem.fluvius.keystore.certificate=/path/to/certificate.cer
spring.ssl.bundle.pem.fluvius.keystore.private-key=/path/to/certificate.key
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_BE_FLUVIUS_BASE_URL=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_OAUTH_TOKEN_URL=https://login.microsoftonline.com/${region-connector.be.fluvius.oauth.tenant-id}/oauth2/V2.0/token
REGION_CONNECTOR_BE_FLUVIUS_OAUTH_CLIENT_ID=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_OAUTH_TENANT_ID=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_OAUTH_CLIENT_SECRET=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_OAUTH_SCOPE=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_SUBSCRIPTIONKEY=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_CONTRACTnUMBER=REPLACE_ME
REGION_CONNECTOR_BE_FLUVIUS_MOCK_MANDATES=false
REGION_CONNECTOR_BE_FLUVIUS_RETRY=0 0 * * * *
REGION_CONNECTOR_BE_FLUVIUS_CHECK_ACCEPTANCE=0 0 * * * *
SPRING_SSL_BUNDLE_PEM_FLUVIUS_KEYSTORE_CERTIFICATE=/path/to/certificate.cer
SPRING_SSL_BUNDLE_PEM_FLUVIUS_KEYSTORE_PRIVATE_KEY=/path/to/certificate.key
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
