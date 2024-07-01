# Region Connector for the Netherlands (Mijn Aansluiting)

This README will guide you through the process of configuring a region connector for the permission
administrators in the Netherlands.
Mijn Aansluiting permission process is based on an OAuth 2.0 Authorization Code Grant with the PKCE extension.
See [OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
and [Key Proof](https://datatracker.ietf.org/doc/html/rfc7636) for more information.
This region connector uses an event sourcing approach to create permission requests with the permission administrator.
It emits the following information:

- Consent Market Documents for changes in statuses for permission requests
- Connection Status Messages for changes in statuses of permission requests
- Validated Historical Data Market Documents for consumption, production energy data and gas data. **IMPORTANT**: All
  data will be missing the first value of the requested period, because it is needed to calculate the deltas between the
  total daily meter readings.

Mijn Aansluiting only supports daily measurements for a maximum of two years in the past, but unlimited time in the
future.

For the OAuth 2.0 flow a private key is required, that has to be made available via a Java Key Store.
The configuration parameters can be seen in
section [Configuration of the Region Connector](#configuration-of-the-region-connector)
To create a Java Key Store see this [tutorial](https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html).
The private key must use the RS256 key algorithm.

The following table shows all the test users that are available on the acceptance environment provided by mijn
aansluiting.

| user login  | verify code | note      |
|-------------|-------------|-----------|
| Testuser 1  | 8           |           |
| Testuser 3  | 19          | unusable  |
| Testuser 4  | 145         |           |
| Testuser 5  | 129         |           |
| Testuser 6  | 1           |           |
| Testuser 7  | 1           |           |
| Testuser 8  | 11          |           |
| Testuser 9  | 3107        |           |
| Testuser 10 | 1807        |           |
| Testuser 11 | 12          | preferred |

## Prerequisites

- Register a user with Mijn Aansluting here: https://www.acc.mijnenergiedata.nl/toestemmingen/welkom
- Get the client-id and client-secret

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                | Description                                                                                                                                                                     |
|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.nl.mijn.aansluiting.issuer-url`   | The Issuer Url of the authorization server. You can get this information from the portal where you registered the user.                                                         |
| `region-connector.nl.mijn.aansluiting.client-id`    | The Client-ID needed to facilitate the OAuth flow.                                                                                                                              |
| `region-connector.nl.mijn.aansluiting.scope`        | The scope of the energy data that is requested, `24_maanden_dagstanden` allows to access up to two year old energy data.                                                        |
| `region-connector.nl.mijn.aansluiting.redirect-url` | The redirect URL that will be used by the authorization server to redirect the final customer to. Should be `domain + /nl-mijn-aansluiting/oauth2/code/mijn-aansluiting`        |
| `region-connector.nl.mijn-aansluting.polling`       | Used to configure when future data should be polled, uses Spring cron syntax. The default is 17 o'clock every day.                                                              |
| `region-connector.nl.mijn.aansluiting.key-id`       | The ID of the key for Mijn Aansluiting.                                                                                                                                         |
| `spring.ssl.bundle.jks.nl.keystore.location`        | Path to the keystore, which contains the private key needed to create OAuth Requests. It is recommended to create a keystore for each key to simplify key rotation and updates. |
| `spring.ssl.bundle.jks.nl.keystore.password`        | Password to access the keystore.                                                                                                                                                |
| `spring.ssl.bundle.jks.nl.key.alias`                | The alias under which the key is saved in the keystore.                                                                                                                         |
| `spring.ssl.bundle.jks.nl.key.password`             | Password to access the key in the keystore                                                                                                                                      |
| `spring.ssl.bundle.jks.nl.keystore.type`            | The keystore type. Should always be set to `JKS`                                                                                                                                |

### .properties file

Example configuration for an `application.properties` file:

```properties
# Key Store Config
spring.ssl.bundle.jks.nl.keystore.location=./mijn-aansluiting.jks
spring.ssl.bundle.jks.nl.keystore.password=password
spring.ssl.bundle.jks.nl.keystore.type=JKS
spring.ssl.bundle.jks.nl.key.alias=mijn-aansluiting
spring.ssl.bundle.jks.nl.key.password=password

# Other configuration
region-connector.nl.mijn.aansluiting.key-id=id
region-connector.nl.mijn.aansluiting.issuer-url=https://example.com
region-connector.nl.mijn.aansluiting.client-id=client-id
region-connector.nl.mijn.aansluiting.scope=24_maanden_dagstanden
region-connector.nl.mijn.aansluiting.redirect-url=https://example.com/callback
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
# Key Store Config
SPRING_SSL_BUNDLE_JKS_NL_KEYSTORE_LOCATION=./mijn-aansluiting_jks
SPRING_SSL_BUNDLE_JKS_NL_KEYSTORE_PASSWORD=password
SPRING_SSL_BUNDLE_JKS_NL_KEYSTORE_TYPE=JKS
SPRING_SSL_BUNDLE_JKS_NL_KEY_ALIAS=mijn-aansluiting
SPRING_SSL_BUNDLE_JKS_NL_KEY_PASSWORD=password

# Other configuration
REGION_CONNECTOR_NL_MIJN_AANSLUITING_KEY_ID=id
REGION_CONNECTOR_NL_MIJN_AANSLUITING_ISSUER_URL=https://example.com
REGION_CONNECTOR_NL_MIJN_AANSLUITING_CLIENT_ID=client-id
REGION_CONNECTOR_NL_MIJN_AANSLUITING_SCOPE=24_maanden_dagstanden
REGION_CONNECTOR_NL_MIJN_AANSLUITING_REDIRECT_URL=https://example.com/callback
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
