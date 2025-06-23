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

For the available test users see [mijnenergiedata.nl](https://www.acc.mijnenergiedata.nl/docs/test-users.html).
To test use the test users with the correct metering point for specific data.
For data needs for electricity data pick the test users whose EAN starts with _ELK_, for gas the EAN needs to start with
_GAS_.

## Prerequisites

- Register a user with Mijn Aansluiting here: https://www.acc.mijnenergiedata.nl/toestemmingen/welkom
- Get the client-id and client-secret
- Generate a JKS key pair and register your public key with Mijn Aansluiting.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                        | Description                                                                                                                                                                     |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.nl.mijn.aansluiting.issuer-url`           | The Issuer Url of the authorization server. You can get this information from the portal where you registered the user.                                                         |
| `region-connector.nl.mijn.aansluiting.continuous-client-id` | The Client-ID needed to facilitate the OAuth flow. Must be the Client-ID for the continuous API.                                                                                |
| `region-connector.nl.mijn.aansluiting.continuous-scope`     | The scope of the energy data that is requested, `24_maanden_dagstanden` allows to access up to two year old energy data.                                                        |
| `region-connector.nl.mijn.aansluiting.continuous-key-id`    | The ID of the key for Mijn Aansluiting continuous API.                                                                                                                          |
| `region-connector.nl.mijn.aansluiting.single-client-id`     | The Client-ID needed to facilitate the OAuth flow. Must be the Client-ID for the single permission API.                                                                         |
| `region-connector.nl.mijn.aansluiting.single-scope`         | The scope of the accounting point data that is requested, `consumption_data` allows to access accounting point data.                                                            |
| `region-connector.nl.mijn.aansluiting.single-key-id`        | The ID of the key for Mijn Aansluiting single permission API.                                                                                                                   |
| `region-connector.nl.mijn.aansluiting.redirect-url`         | The redirect URL that will be used by the authorization server to redirect the final customer to. Should be `domain + /nl-mijn-aansluiting/oauth2/code/mijn-aansluiting`        |
| `region-connector.nl.mijn-aansluting.polling`               | Used to configure when future data should be polled, uses Spring cron syntax. The default is 17 o'clock every day.                                                              |
| `spring.ssl.bundle.jks.nl.keystore.location`                | Path to the keystore, which contains the private key needed to create OAuth Requests. It is recommended to create a keystore for each key to simplify key rotation and updates. |
| `spring.ssl.bundle.jks.nl.keystore.password`                | Password to access the keystore.                                                                                                                                                |
| `spring.ssl.bundle.jks.nl.key.alias`                        | The alias under which the key is saved in the keystore.                                                                                                                         |
| `spring.ssl.bundle.jks.nl.key.password`                     | Password to access the key in the keystore                                                                                                                                      |
| `spring.ssl.bundle.jks.nl.keystore.type`                    | The keystore type. Should always be set to `JKS`                                                                                                                                |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
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
region-connector.nl.mijn.aansluiting.polling=0 0 17 * * *
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
