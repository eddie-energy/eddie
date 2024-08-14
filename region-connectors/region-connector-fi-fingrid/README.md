# Region Connector for Finland (Fingrid)

This README will guide you through the process of configuring a region connector for Finland.

## Prerequisites

- Register a user with Fingrid [here](https://fingrid.fi).
- Download the certificate from Fingrid.
- Create a Java Keystore containing the certificate.
  To create a Java Keystore see this [tutorial](https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html).

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                            | Description                                                                                                                                                             |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.fi.fingrid.organisation-user` | Identifier of the organisation that was created.                                                                                                                        |
| `region-connector.fi.fingrid.organisation-name` | Name of the organisation, only used to inform final customers about the organisation.                                                                                   |
| `region-connector.fi.fingrid.api-url`           | API URL of the Fingrid API                                                                                                                                              |
| `spring.ssl.bundle.jks.fi.keystore.location`    | Path to the keystore, which contains the private key needed to sign requests. It is recommended to create a keystore for each key to simplify key rotation and updates. |
| `spring.ssl.bundle.jks.fi.keystore.password`    | Password to access the keystore.                                                                                                                                        |
| `spring.ssl.bundle.jks.fi.key.alias`            | The alias under which the key is saved in the keystore.                                                                                                                 |
| `spring.ssl.bundle.jks.fi.key.password`         | Password to access the key in the keystore                                                                                                                              |
| `spring.ssl.bundle.jks.fi.keystore.type`        | The keystore type. Should always be set to `JKS`                                                                                                                        |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.fi.fingrid.organisation-user=0000000000000.THP
region-connector.fi.fingrid.organisation-name=Organisation Name
region-connector.fi.fingrid.api-url=https://dh-fingrid-cert01-b2b.azurewebsites.net/rest/FGR/
spring.ssl.bundle.jks.fingrid.keystore.location=/path/to/keystore.jks
spring.ssl.bundle.jks.fingrid.keystore.password=password
spring.ssl.bundle.jks.fingrid.keystore.type=JKS
spring.ssl.bundle.jks.fingrid.key.alias=fingrid
spring.ssl.bundle.jks.fingrid.key.password=password
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_FI_FINGRID_ORGANISATION_USER=0000000000000_THP
REGION_CONNECTOR_FI_FINGRID_ORGANISATION_NAME=Organisation Name
REGION_CONNECTOR_FI_FINGRID_API_URL=https://dh-fingrid-cert01-b2b.azurewebsites.net/rest/FGR/
SPRING_SSL_BUNDLE_JKS_FINGRID_KEYSTORE_LOCATION=/path/to/keystore_jks
SPRING_SSL_BUNDLE_JKS_FINGRID_KEYSTORE_PASSWORD=password
SPRING_SSL_BUNDLE_JKS_FINGRID_KEYSTORE_TYPE=JKS
SPRING_SSL_BUNDLE_JKS_FINGRID_KEY_ALIAS=fingrid
SPRING_SSL_BUNDLE_JKS_FINGRID_KEY_PASSWORD=password
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
