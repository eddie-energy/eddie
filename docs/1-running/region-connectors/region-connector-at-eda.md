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

| Configuration values                                                          | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `region-connector.at.eda.eligibleparty.id `                                   | The company identification number you get from registering as a service provider with ebUtillitis. This can either be the normal Eligible Party ID or for energy communities the energy community partner ID.                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.conversation-id.prefix`                              | Optional prefix for generated EDA grouping IDs used as both message and conversation IDs, for example to route multiple EDDIE adapters using the same eligible party. The prefix may contain only `0-9`, `A-Z`, and `a-z`. The connector fails to start if the prefix contains other characters or if a complete generated ID would exceed the EDA limit of 35 characters.                                                                                                          |
| `region-connector.at.eda.energy-community-id`                                 | The ID of the energy community in this format `ATCC0000DYNAMXX000000000000000000`. This property is optional, if omitted the EDDIE instance will not support energy community specific processes, such as the [CESUJoinRegquest](../../2-integrating/data-needs.md#CESUJoinRequestDataNeed).                                                                                                                                                                                        |
| `region-connector.at.eda.retry`                                               | The retry configuration, when a failed request should be retried uses spring cron syntax.                                                                                                                                                                                                                                                                                                                                                                                           |
| `region-connector.at.eda.ponton.messenger.adapter.id`                         | The Id the region connector uses to connect with the PontonXP Messenger. This should match the default value configured in the messenger.                                                                                                                                                                                                                                                                                                                                           |
| `region-connector.at.eda.ponton.messenger.adapter.version`                    | Version number of the adapter                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.ponton.messenger.hostname`                           | URL or IP address of your PontonXP Messenger.                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.ponton.messenger.port`                               | The port that is used for connecting to the messenger. If you did not change this in the `server.xml` file of the messenger it should be `2600`.                                                                                                                                                                                                                                                                                                                                    |
| `region-connector.at.eda.ponton.messenger.api.endpoint`                       | Endpoint of the XP Messenger REST API. Default should be `<hostname>[:<api-port>]/api`                                                                                                                                                                                                                                                                                                                                                                                              |
| `region-connector.at.eda.ponton.messenger.folder`                             | Folder that is used to store information that the adapter needs for operating. This folder stores the `id.dat` file that is generated when the region connector first connects to the PontonXP Messenger. This file is used by the messenger to authenticate the adapter, i.e. all subsequent instances of the same adapter (same adapter id) need this file if they want to connect to the messenger.                                                                              |
| `region-connector.at.eda.ponton.messenger.username`                           | Username that can be used to retrieve a JWT Token from the PontonXP Messengers authentication endpoint. This is needed in order to use some of the authentication protected REST API endpoints. The given user must not be additionally secured by 2FA.                                                                                                                                                                                                                             |
| `region-connector.at.eda.ponton.messenger.password`                           | Password for the above username. Needed to retrieve a JWT Token from the PontonXP Messengers authentication endpoint. This is needed in order to use some of the authentication protected REST API endpoints.                                                                                                                                                                                                                                                                       |
| `region-connector.at.eda.ponton.messenger.inbound-connections`                | Expected number of inbound WebSocket connections to the PontonXP Messenger. Must be greater than `0`. Defaults to `1`.                                                                                                                                                                                                                                                                                                                                                              |
| `region-connector.at.eda.ponton.messenger.outbound-connections`               | Expected number of outbound WebSocket connections to the PontonXP Messenger. Must be greater than `0`. Defaults to `1`.                                                                                                                                                                                                                                                                                                                                                             |
| `region-connector.at.eda.ponton.messenger.connection-watchdog.interval`       | How often the region connector checks the adapter's WebSocket connection state. Must be a positive Spring Boot duration. Defaults to `1m`.                                                                                                                                                                                                                                                                                                                                          |
| `region-connector.at.eda.ponton.messenger.connection-watchdog.status-timeout` | Maximum time after the adapter starts or reconnects in which the PontonXP Messenger must report its connection status. If no status callback arrives within this period, or the reported inbound or outbound connection count is below the configured expectation, the watchdog reconnects the adapter. Must be a positive Spring Boot duration. Defaults to `2m`.                                                                                                                  |
| `region-connector.at.eda.consumption.records.remove.duplicates.enabled`       | Enables the removal of duplicate consumption records that were already received once by EDDIE. If disabled duplicate consumption data for permission requests concerning the same metering point will be emitted to the outbound connector, this is also true if the permission request is already considered fulfilled, since MDA will send updates for the data. If enabled those updates will not be received. To enable set to `true`, disabled otherwise. Disabled by default. |
| `region-connector.at.eda.ponton.messenger.enabled`                            | Default `true`. If set to true or missing the EDA Region Connector will try to connect to the configured Ponton X/P Messenger instance. If set to `false` it will use a drop-in replacement, which is a simple REST API to manually send Market Messages, should be used for testing and developement only, never in production!                                                                                                                                                    |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.at.eda.eligibleparty.id=EP123456
region-connector.at.eda.energy-community-id=ATCC0000DYNAMXX000000000000000000
region-connector.at.eda.conversation-id.prefix=EDDIE1
region-connector.at.eda.retry=0 0 */1 * * *
region-connector.at.eda.ponton.messenger.adapter.id=Eddie
region-connector.at.eda.ponton.messenger.adapter.version=1.0.0
region-connector.at.eda.ponton.messenger.hostname=pontonxp.messenger.com
region-connector.at.eda.ponton.messenger.port=2600
region-connector.at.eda.ponton.messenger.api.endpoint=pontonxp.messenger.com/api
region-connector.at.eda.ponton.messenger.folder=/opt/pontonxp
region-connector.at.eda.ponton.messenger.username=username
region-connector.at.eda.ponton.messenger.password=password
region-connector.at.eda.ponton.messenger.inbound-connections=1
region-connector.at.eda.ponton.messenger.outbound-connections=1
region-connector.at.eda.ponton.messenger.connection-watchdog.interval=1m
region-connector.at.eda.ponton.messenger.connection-watchdog.status-timeout=2m
region-connector.at.eda.consumption.records.remove.duplicates.enabled=false
region-connector.at.eda.ponton.messenger.enabled=true
```

The duration values use Spring Boot's duration format. For example, `30s`, `1m`, and `2m` mean 30 seconds, one minute,
and two minutes respectively.

The connection count and watchdog values configure the connection watchdog for the PontonXP Messenger WebSocket
connection.

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.

## Consents for multiple metering points

An EDA acceptance can grant access to several metering points for one request.
The connector maintains one permission request per granted metering point and consent ID.
These permissions retain the original connection ID, data need ID, DSO, conversation, permission timeframe,
granularity, energy direction and participation factor.
Additional permissions do not send another consent request to the customer.

The connector matches an existing consent before creating a permission.
Repeated, reordered and partial acceptance messages therefore reuse the existing assignments, including after a restart.
Concurrent acceptance messages are serialized using a database lock on the request's oldest event.
The complete event sequence is committed in one transaction and published only after that transaction commits.
The outbox retains its existing in-process publication mechanism; it does not guarantee publication after a process
failure between the database commit and event delivery.

When the initial request specifies a metering point, its permission is assigned to that point even if another point
appears first in the acceptance message.
An acceptance received before the receipt acknowledgement records `SENT_TO_PERMISSION_ADMINISTRATOR` before `ACCEPTED`.
A later acknowledgement cannot move an accepted permission back to its previous state.
Additional consents can be processed while a sibling permission is accepted or fulfilled.
An existing revoked, terminated or timed-out permission is never reactivated by a repeated acceptance.

An exact conversation match takes precedence over the CM request ID.
Acceptance matching uses the consent request conversation recorded by validation, even if a subsequent termination
has replaced the permission's current conversation ID.
The CM request ID is only used as a fallback if it identifies one request context.
The connector rejects conflicting consent-to-point assignments and ambiguous request contexts.
Incomplete acceptance entries are ignored without creating permission events; valid entries in the same message
can still be processed.
Warnings and errors identify messages that require investigation and manual replay after reconciliation.

### Existing inconsistent permission events

Versions affected by the multiple-point acceptance defect could append AcceptedEvents for several points to the
original permission, leaving additional permissions in `SENT_TO_PERMISSION_ADMINISTRATOR`.
The corrected handler detects these mixed streams and refuses to extend them automatically.
Installing this fix does not rewrite existing permission events or request a new customer consent.

The following read-only query identifies mixed acceptance streams:

```sql
SELECT permission_id,
       count(DISTINCT metering_point_id) AS metering_points,
       count(DISTINCT cm_consent_id) AS consents
FROM at_eda.permission_event
WHERE dtype = 'AcceptedEvent'
GROUP BY permission_id
HAVING count(DISTINCT metering_point_id) > 1
    OR count(DISTINCT cm_consent_id) > 1;
```

Reconciliation must be prepared and reviewed separately from deployment.
Use the original AcceptedEvents and, where necessary, the received EDA messages as evidence, rather than the current
permission projection alone. Record source event IDs, checksums, original and target permission IDs, consent IDs,
metering points, connection IDs, data need IDs and effective permission periods in an audit manifest.
Reuse existing child permissions where their origin is unambiguous. Preserve original evidence and later revocations
or terminations. Recalculate fulfillment from data for the individual point instead of copying a shared fulfilled state.
The reconciliation must be idempotent and must not publish consent requests, revocations or retransmission requests.
Never mark all pending permissions accepted solely because they share a conversation or data need.

## Retransmission of missing data

Submit a [Redistribution Transaction Request](../../2-integrating/messages/cim/redistribution-transaction-request-documents.md)
for the permission of the individual metering point.
The permission must be `ACCEPTED` or `FULFILLED`, with both a metering point and a consent ID.
Revoked or externally terminated permissions cannot be used for retransmission.
Consent revocation transitions an `ACCEPTED` permission to `REVOKED`, following the
[permission process model](../../2-integrating/permission-requests.md#permission-process-model).
The requested interval must be non-empty, ordered and within the permission timeframe.
An open-ended permission has no upper permission-date limit.
The connector represents the requested interval using local midnight boundaries in `Europe/Vienna`, with an exclusive end.

CR_REQ_PT process version `04.30` continues to use transport schemaset `CR_REQ_PT_03.00`, message type
`ANFORDERUNG_PT` and transport version `03.00`. The XML payload uses CPRequest schema version `01.12`.
Do not replace the transport version with the process version.
See the [ebUtilities process specification](https://www.ebutilities.at/prozesse/717).

Each retransmission starts a new conversation. Requested CR_MSG data uses that request's conversation ID,
which can differ from the original consent conversation.
Incoming consumption records are associated with permissions by metering point and covered date.
Audits must include retransmission conversations when checking received data.
An `ANTWORT_PT` response with code `70` confirms acceptance of the request, not completeness of the returned data.
Code `94` reports that no data is available for the requested period.
Verify the actual returned intervals and their downstream delivery before considering a gap recovered.

## Importing Permission Requests

The region connector provides a REST API to import permission requests for consents that were already granted outside EDDIE.
The imported permission request is immediately marked as `ACCEPTED`, so the normal permission request process (sending the request to the DSO and waiting for the consent) is skipped.
Only permission requests for the data needs [`ValidatedHistoricalDataDataNeed`](../../2-integrating/data-needs.md#validatedhistoricaldatadataneed) and [`CESUJoinRequestDataNeed`](../../2-integrating/data-needs.md#cesujoinrequestdataneed) are supported.

The endpoint can only be used by the eligible party; therefore, it is only available via the management port.

| Field               | Description                                                                                                                                                                                                |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `connectionId`      | Connection id of the final customer's connection.                                                                                                                                                          |
| `meteringPointId`   | Id of the metering point the permission request is for. Needs to be exactly 33 characters long.                                                                                                            |
| `dataNeedId`        | Id of the data need the permission request is for.                                                                                                                                                         |
| `dsoId`             | Company identification number of the DSO, needs to be exactly 6 characters long (e.g. `AT001000`).                                                                                                         |
| `consentId`         | Id of the consent that was already granted by the final customer to the eligible party.                                                                                                                    |
| `creationDateTime`  | Timestamp of the consent, needs to be in the past. It is used to calculate the permission timeframe of the data need.                                                                                      |
| `meterReadingStart` | _Optional_. Start of the timeframe the meter reading was taken. Can only be supplied together with `meterReadingEnd`, must not be after it, and must be within the permission timeframe of the data need.  |
| `meterReadingEnd`   | _Optional_. End of the timeframe the meter reading was taken. Can only be supplied together with `meterReadingStart`, must not be before it, and must be within the permission timeframe of the data need. |

If the import was successful, the endpoint returns `201 Created` with the permission id of the imported permission request in the response body.
The `Location` header contains the URL of the [connection status message](../../2-integrating/messages/cim/connection-status-messages.md) stream for the imported permission request, where updates for the permission request can be consumed.

```http request
### Import an existing permission request
POST http://localhost:${eddie.management.server.port}/region-connectors/at-eda/${eddie.management.server.urlprefix}/permission-request/import
Content-Type: application/json

{
  "connectionId": "1",
  "meteringPointId": "AT0020000000000000000000021247179",
  "dataNeedId": "3dd39c06-253e-4c95-a255-e3eefc69f613",
  "dsoId": "AT002000",
  "consentId": "AT002000202608281020381218219898190",
  "creationDateTime": "2025-08-28T00:00:00Z"
}
```

## Configuring PontonXP Messenger with multiple eligible parties and adapters

If you are in a situation, where you have multiple local partners (eligible parties) using the same PontonXP Messenger and want to route messages to specific adapters based on the eligible party id,
some additional setup is needed in the messenger configuration.

The way that the messenger works, is that it requires an agreement between all partners that want to communicate.
This means there needs to be an agreement between every local partner id (eligible party id) and every DSO that want to exchange data.
Incoming messages are then routed to the adapter that is specified in the agreement.
These Agreements can be created either manually or are automatically created when the first message from a local partner is sent to a DSO.
The Agreements are always created based on a template that configures how communication between the partners is supposed to work.

These templates can be customized using the Apache Velocity template engine.

To customize the agreement template that is used for the austrian market, create a file called `EDA_private.vm` in
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
