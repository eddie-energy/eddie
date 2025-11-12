# Region Connector for Austria (EDA)

This README will guide you through the process of configuring a region connector for Austria.

## Prerequisites

- Register as a service provider at [ebUtilities.at](https://www.ebutilities.at/registrierung) in order to get a
  company identification number (e.g. `EP100129`).
- Download and set up a PontonXP Messenger instance (
  see [PontonXP Messenger](https://www.ponton.de/ponton-x-p-licensing-download)). This is a licensed product, so you
  will need to contact Ponton for a license, they will also help you with the setup.
- After you have set up the PontonXP Messenger, you should change the default adapter of the messenger. The default
  adapter is used for inbound messages that arrive asynchronously. The default adapter is configured via
  the `<DefaultAdapterId>TestAdapter</DefaultAdapterId>` section in
  the `messenger.xml` file found in the config folder of the PontonXP Messenger. Change this value to something else,
  e.g. `<DefaultAdapterId>Eddie</DefaultAdapterId>`, the value should match the config value that is later passed to
  the region connector. If you have multiple services with their own eligible party id and want to route messages to specific adapters based on the eligible party id, see the section [Configuring PontonXP Messenger with multiple eligible parties and adapters](#configuring-pontonxp-messenger-with-multiple-eligible-parties-and-adapters).

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                                    | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.at.eda.eligibleparty.id `                             | The company identification number you get from registering as a service provider with ebUtillitis.                                                                                                                                                                                                                                                                                                                                                                                  |
| `region-connector.at.eda.retry`                                         | The retry configuration, when a failed request should be retried uses spring cron syntax.                                                                                                                                                                                                                                                                                                                                                                                           |
| `region-connector.at.eda.ponton.messenger.adapter.id`                   | The Id the region connector uses to connect with the PontonXP Messenger. This should match the default value configured in the messenger.                                                                                                                                                                                                                                                                                                                                           |
| `region-connector.at.eda.ponton.messenger.adapter.version`              | Version number of the adapter                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.ponton.messenger.hostname`                     | URL or IP address of your PontonXP Messenger.                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.ponton.messenger.port`                         | The port that is used for connecting to the messenger. If you did not change this in the `server.xml` file of the messenger it should be `2600`.                                                                                                                                                                                                                                                                                                                                    |
| `region-connector.at.eda.ponton.messenger.api.endpoint`                 | Endpoint of the XP Messenger REST API. Default should be `<hostname>[:<api-port>]/api`                                                                                                                                                                                                                                                                                                                                                                                              |                                                                                                                                                                 
| `region-connector.at.eda.ponton.messenger.folder`                       | Folder that is used to store information that the adapter needs for operating. This folder stores the `id.dat` file that is generated when the region connector first connects to the PontonXP Messenger. This file is used by the messenger to authenticate the adapter, i.e. all subsequent instances of the same adapter (same adapter id) need this file if they want to connect to the messenger.                                                                              |
| `region-connector.at.eda.ponton.messenger.username`                     | Username that can be used to retrieve a JWT Token from the PontonXP Messengers authentication endpoint. This is needed in order to use some of the authentication protected REST API endpoints. The given user must not be additionally secured by 2FA.                                                                                                                                                                                                                             |
| `region-connector.at.eda.ponton.messenger.password`                     | Password for the above username. Needed to retrieve a JWT Token from the PontonXP Messengers authentication endpoint. This is needed in order to use some of the authentication protected REST API endpoints.                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.consumption.records.remove.duplicates.enabled` | Enables the removal of duplicate consumption records that were already received once by EDDIE. If disabled duplicate consumption data for permission requests concerning the same metering point will be emitted to the outbound connector, this is also true if the permission request is already considered fulfilled, since MDA will send updates for the data. If enabled those updates will not be received. To enable set to `true`, disabled otherwise. Disabled by default. |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.at.eda.eligibleparty.id=EP123456
region-connector.at.eda.retry=0 0 */1 * * *
region-connector.at.eda.ponton.messenger.adapter.id=Eddie
region-connector.at.eda.ponton.messenger.adapter.version=1.0.0
region-connector.at.eda.ponton.messenger.hostname=pontonxp.messenger.com
region-connector.at.eda.ponton.messenger.port=2600
region-connector.at.eda.ponton.messenger.api.endpoint=pontonxp.messenger.com/api
region-connector.at.eda.ponton.messenger.folder=/opt/pontonxp
region-connector.at.eda.ponton.messenger.username=username
region-connector.at.eda.ponton.messenger.password=password
region-connector.at.eda.consumption.records.remove.duplicates.enabled=false
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.

## Configuring PontonXP Messenger with multiple eligible parties and adapters

If you are in a situation, where you have multiple local partners (eligible parties) using the same PontonXP Messenger and want to route messages to specific adapters based on the eligible party id,
some additional setup is needed in the messenger configuration.

The way that the messenger works, is that it requires an agreement between all partners that want to communicate.
This means there needs to be an agreement between every local partner id (eligible party id) and every DSO that want to exchange data.
Incoming messages are then routed to the adapter that is specified in the agreement.
These Agreements can be created either manually or are automatically created when the first message from a local partner is sent to a DSO.
The Agreements are always created based on a template that configures how communication between the partners is supposed to work.

These templates can be customized using the Apache Velocity template engine.

To customize the agreement template that is used for the austrian market, create a file called  `EDA_private.vm` in
`config/agreementTemplates/` of the messenger installation.

The following is an example of a template that can be used to set the
`DefaulAdapterId` based on the local partner id (eligible party id):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<PrivateCollaborationAgreement TemplateId="EDA">
    #if ($agreement.ownPartner.backendPartnerId == "ELIGIBLE_PARTY_ID_1")
    <DefaultAdapterId>ELIGIBLE_PARTY_ID_1_Adapter</DefaultAdapterId>
    #elseif ($agreement.ownPartner.backendPartnerId == "ELIGIBLE_PARTY_ID_2")
    <DefaultAdapterId>ELIGIBLE_PARTY_ID_2_Adapter</DefaultAdapterId>
    #else
    <DefaultAdapterId>${agreement.adapterId}</DefaultAdapterId>
    #end
    <Rules SenderRefId="${agreement.ownPartner.id}" ReceiverRefId="${agreement.communicationPartner.id}">
    </Rules>
    <Rules SenderRefId="${agreement.communicationPartner.id}" ReceiverRefId="${agreement.ownPartner.id}">
    </Rules>
</PrivateCollaborationAgreement>       
```

This will ensure that messages that are send to the `ELIGIBLE_PARTY_ID_1` will be sent to the
`ELIGIBLE_PARTY_ID_1_Adapter`,
messages sent to `ELIGIBLE_PARTY_ID_2` will be sent to the
`ELIGIBLE_PARTY_ID_2_Adapter` and all other messages will be sent to the default adapter.
Replace these values accordingly.

After creating the template, restart the messenger to apply the changes.

To test if this works, try to manually create an agreement between 2 Partner through the web interface of the messenger.
This can be done under `messenger/agreements` =>
`Add agreement` in the top left. Then select one of the local partner and any remote partner.
Make sure the Agreement template is set to `EDA` and continue.
If you scroll down to the `Integration` section, it should show the expected Adapter in the `Default-Adapter` field.

If the Agreement template is showing no content when trying to manually create it, it means that the template
`EDA_private.vm` is invalid.

This will only affect new agreements that are created, existing agreements will not be changed.
You can either change the adapter of an existing agreement manually or delete the agreement and let it be recreated.