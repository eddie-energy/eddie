# Region Connector for Austria (EDA)

This README will guide you through the process of configuring a region connector for Austria.

## Prerequisites

- Register as a service provider at [ebUtilities.at](https://www.ebutilities.at/registrierung) in order to get a
  company identification number (e.g. `EP100129`).
- Download and set up a PontonXP Messenger instance (
  see [PontonXP Messenger](https://www.ponton.de/ponton-x-p-licensing-download/)). This is a licensed product, so you
  will need to contact Ponton for a license, they will also help you with the setup.
- After you have set up the PontonXP Messenger, you should change the default adapter of the messenger. The default
  adapter is used for inbound messages that arrive asynchronously. The default adapter is configured via
  the `<DefaultAdapterId>TestAdapter</DefaultAdapterId>` section in
  the `messenger.xml` file found in the config folder of the PontonXP Messenger. Change this value to something else,
  e.g. `<DefaultAdapterId>Eddie</DefaultAdapterId>`, the value should match the config value that is later passed to
  the region connector.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                       | Describtion                                                                                                                                                                                                                                                                                                                                                                                            |
|------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.at.eda.eligibleparty.id `                | The company identification number you get from registering as a service provider with ebUtillitis,                                                                                                                                                                                                                                                                                                     |
| `region-connector.at.eda.ponton.messenger.adapter.id`      | The Id the region connector uses to connect with the PontonXP Messenger. This should match the default value configured in the messenger.                                                                                                                                                                                                                                                              |
| `region-connector.at.eda.ponton.messenger.adapter.version` | Version number of the adapter                                                                                                                                                                                                                                                                                                                                                                          |
| `region-connector.at.eda.ponton.messenger.hostname`        | URL or IP address of your PontonXP Messenger.                                                                                                                                                                                                                                                                                                                                                          
| `region-connector.at.eda.ponton.messenger.port`            | The port that is used for connecting to the messenger. If you did not change this in the `server.xml` file of the messenger it should be `2600`.                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.ponton.messenger.folder`          | Folder that is used to store information that the adapter needs for operating. This folder stores the `id.dat` file that is generated when the region connector first connects to the PontonXP Messenger. This file is used by the messenger to authenticate the adapter, i.e. all subsequent instances of the same adapter (same adapter id) need this file if they want to connect to the messenger. |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.at.eda.eligibleparty.id=EP123456
region-connector.at.eda.ponton.messenger.adapter.id=Eddie
region-connector.at.eda.ponton.messenger.adapter.version=1.0.0
region-connector.at.eda.ponton.messenger.hostname=pontonxp.messenger.com
region-connector.at.eda.ponton.messenger.port=2600
region-connector.at.eda.ponton.messenger.folder=/opt/pontonxp
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_AT_EDA_ELIGIBLEPARTY_ID=EP123456
REGION_CONNECTOR_AT_EDA_PONTON_MESSENGER_ADAPTER_ID=Eddie
REGION_CONNECTOR_AT_EDA_PONTON_MESSENGER_ADAPTER_VERSION=1.0.0
REGION_CONNECTOR_AT_EDA_PONTON_MESSENGER_HOSTNAME=pontonxp.messenger.com
REGION_CONNECTOR_AT_EDA_PONTON_MESSENGER_PORT=2600
REGION_CONNECTOR_AT_EDA_PONTON_MESSENGER_FOLDER=/opt/pontonxp
```

## Running the Region Connector via the framework

If you are using the framework, the region connector should appear in the list of available
region connectors if it has been configured correctly.