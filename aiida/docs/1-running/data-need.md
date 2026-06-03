# Data Needs

The general concept of data needs is explained in the [EDDIE documentation](https://architecture.eddie.energy/framework/2-integrating/data-needs.html).

AIIDA supports two types of data needs: inbound and outbound. Outbound data needs are used to define the data that AIIDA should provide to EPs, while inbound data needs are used to define the data that AIIDA should consume from EPs.

## Fields

| Field                      | Type     | Description                                                                                                           |
|----------------------------|----------|-----------------------------------------------------------------------------------------------------------------------|
| `type`                     | String   | Type of the data need, either `outbound-aiida` or `inbound-aiida`.                                                    |
| `id`                       | String   | Unique id that can be used to reference this data need.                                                               |
| `name`                     | String   | Short memorable name of the data need that may be presented to the customer.                                          |
| `description`              | String   | Multiline string that describes this data need in a human readable form to be shown in the UI.                        |
| `purpose`                  | String   | Multiline string that describes the purpose of this data need.                                                        |
| `policyLink`               | URL      | URL to the data policy that applies to this data need.                                                                |
| `enabled`                  | boolean  | Enables or disables a data need.                                                                                      |
| `duration`                 | Object   | Describes the timeframe for this data need.                                                                           |
| `transmissionSchedule`     | String   | Cron expression that defines how often the data should be sent or received.                                           |
| `allowTransmissionControl` | boolean  | Whether the EP may remotely control transmission. See [Transmission Control](#transmission-control). Default `false`. |
| `acknowledgementRequired`  | boolean  | Whether an acknowledgement is required for the data transmission. See [Acknowledgement](#acknowledgement).            |
| `schemas`                  | String[] | Schemas that define the structure and format of the data, e.g. standardized schemas such as CIM or custom schemas.    |
| `asset`                    | String   | The asset that the data need is associated with, linking the data to specific physical or logical assets.             |
| `dataTags`                 | String[] | Data tags that further specify the type of data that is expected.                                                     |

## Outbound

> [!NOTE]
> AIIDA &rarr; EP

```json
{
  "type": "outbound-aiida",
  "id": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
  "name": "FUTURE_NEAR_REALTIME_DATA_OUTBOUND",
  "description": "Near realtime consumption data from the smart meter",
  "purpose": "purpose",
  "policyLink": "https://example.com/toc",
  "duration": {
    "type": "relativeDuration",
    "start": "P0D",
    "end": "P10D"
  },
  "transmissionSchedule": "*/5 * * * * *",
  "allowTransmissionControl": false,
  "acknowledgementRequired": false,
  "schemas": [
    "SMART-METER-P1-RAW",
    "SMART-METER-P1-CIM-V1-04",
    "SMART-METER-P1-CIM-V1-12"
  ],
  "asset": "CONNECTION-AGREEMENT-POINT",
  "dataTags": [
    "1-0:1.8.0",
    "1-0:1.7.0"
  ]
}
```

## Inbound

> [!NOTE]
> EP &rarr; AIIDA

```json
{
  "type": "inbound-aiida",
  "id": "f7698978-b9fe-40c8-aebe-c997f7f58f2f",
  "name": "FUTURE_MIN_MAX_ENVELOPE_INBOUND",
  "description": "Inbound reference energy curve min-max operating envelopes for flexible connection agreements",
  "purpose": "purpose",
  "policyLink": "https://example.com/toc",
  "duration": {
    "type": "relativeDuration",
    "start": "P0D",
    "end": "P10D"
  },
  "transmissionSchedule": "*/5 * * * * *",
  "allowTransmissionControl": false,
  "acknowledgementRequired": true,
  "schemas": [
    "MIN-MAX-ENVELOPE-CIM-V1-12",
    "OPAQUE"
  ],
  "asset": "CONNECTION-AGREEMENT-POINT",
  "dataTags": []
}
```

## Acknowledgement

If the `acknowledgementRequired` field is set to `true`, the receiving party (either AIIDA or the EP) is expected to send an acknowledgement message back to the sender after successfully receiving and processing the data.
This acknowledgement message serves as a confirmation that the data has been received and processed correctly, and can be used to ensure reliable communication between AIIDA and EPs.

The acknowledgement is available in the outbound connector being used.

## Transmission Control

If the `allowTransmissionControl` field is set to `true`, the EP may remotely control transmission for the
permission by sending permission commands. Two commands are accepted:

- `SET_TRANSMISSION_ENABLED` - enables or disables transmission.
- `UPDATE_SCHEDULE` - adjusts the transmission schedule (capped at the data-need frequency).

If the field is `false` (the default), these commands are rejected. The `TERMINATE` command is always accepted,
regardless of this flag.

For more details, see:

- [Acknowledgement Market Document](https://architecture.eddie.energy/framework/2-integrating/messages/cim/acknowledgement-market-documents.html#acknowledgement-market-document)
- [EP Subscribing to Acknowledgement](https://architecture.eddie.energy/aiida/1-running/data-sources/mqtt/inbound/inbound-data-source.html#ep-subscribing-to-acknowledgement)