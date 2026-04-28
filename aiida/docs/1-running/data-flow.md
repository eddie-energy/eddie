# Data Flow

This section describes the data flow within **AIIDA** - from data acquisition via data sources, through collection, parsing, and aggregation, to the final streaming process toward **EDDIE**.

![Sequence Diagram of the core components](../diagrams/data-flow.svg)

## Data Source

A **data source** is the logical abstraction of any system that provides data to AIIDA.
It can represent either a physical device, such as a smart meter adapter, or a virtual source, such as the [CIM](data-sources/mqtt/cim/cim-data-source.md) or [Simulation](data-sources/interval/simulation/simulation-data-source.md) data sources.

In AIIDA, data sources are represented by the `DataSource` entity in the database.
Each data source stores the configuration required to establish a connection between the source system and AIIDA.

## Data Source Adapter

The **data source adapter** is responsible for connecting to the data source and collecting raw data.
Using the configuration stored in the corresponding entity, the adapter establishes the connection, retrieves the raw data, and parses it into AIIDA’s internal format.
The parsed data is then forwarded to a data sink, which serves as input for the aggregators.

When AIIDA starts, an adapter is created for each configured and enabled data source.

## Aggregator

The **aggregator** maintains a list of all active data source adapters in memory and provides methods to add or remove adapters at runtime.

The **streamer manager** retrieves filtered and buffered data for each permission (and its corresponding data source) from the aggregator.

### Buffering

The aggregator buffers incoming data based on the **transmission interval** (defined in cron format) for each permission.
Currently, no aggregation logic (such as averaging or summing) is applied - for each key (OBIS code) in the incoming data, only the **last received value** within the interval is forwarded.

#### Example

If the transmission interval is set to _every 5 minutes_, the aggregator collects all incoming data during that time and forwards the **most recent value** of each OBIS code once the interval elapses.

### Filtering

Before forwarding data, the aggregator applies the following filters:

- The asset of the data source matches the asset defined in the permission.
- The data record is not empty.
- The permission has not expired.
- The data originates from the data source specified in the permission.
- The data belongs to the correct user.
- If the permission defines specific OBIS codes, only those are forwarded.

## Permission Scheduler

The **permission scheduler** is responsible for starting and stopping streamers for individual permissions.
If a permission has a future start time, the scheduler sets a timer to start the corresponding streamer at that time.
When a permission expires, the scheduler stops its streamer and removes it from the streamer manager.

## Streamer

The **streamer manager** handles all active streamers within AIIDA.
It keeps an in-memory list of streamers and provides methods to create and stop them.

When a streamer is created for a permission, it retrieves the filtered and buffered data for the corresponding data source from the aggregator.
The streamer then transmits this data to **EDDIE** via **MQTT**, using the configuration defined in the permission’s MqttStreamingConfig.

Currently, AIIDA only supports **MQTT** as the streaming protocol - however, the system is designed to be **easily extendable** to other streaming mechanisms.

## EDDIE ↔ AIIDA Handshake Protocol

### 1. QR Code Payload

QR code created by EDDIE and scanned by AIIDA:

```json
{
  "eddieId": "56faf485-ed0a-4066-8cce-dc227c1350a9",
  "permissionIds": [
    "09520290-ea60-4422-a54c-db8fde93cfd5"
  ],
  "handshakeUrl": "https://framework.eddie.energy/region-connectors/aiida/permission-request/{permissionId}",
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3Nzc5ODE1MjEsImlhdCI6MTc3NzM3NjcyMSwicGVybWlzc2lvbnMiOnsiYWlpZGEiOlsiMDk1MjAyOTAtZWE2MC00NDIyLWE1NGMtZGI4ZmRlOTNjZmQ1Il19fQ.Lo7ttkcQVp-QClKRbyQOCpG_rrB-xpbwHM4T7Mh3FKg"
}
```

### 2. AIIDA → EDDIE: Fetch Permission Details

#### Request

```
GET {handshakeUrl}
Authorization: Bearer {accessToken}
```

#### Response

```json
{
  "eddie_id": "56faf485-ed0a-4066-8cce-dc227c1350a9",
  "permission_request": {
    "connection_id": "b77f7f5c-3d1b-4597-848f-2001700c228b",
    "data_need_id": "fdeef3a6-aa35-4de2-aec6-9072ccd843b2",
    "permission_id": "09520290-ea60-4422-a54c-db8fde93cfd5",
    "start": "2026-04-28",
    "end": "2026-07-28"
  },
  "data_need": {
    "type": "outbound-aiida",
    "acknowledgementRequired": false,
    "asset": "CONNECTION-AGREEMENT-POINT",
    "createdAt": "2026-01-07T06:57:18.773378Z",
    "dataTags": [],
    "description": "Data sent each 2 secs",
    "duration": {
      "type": "relativeDuration",
      "start": "P0D",
      "end": "P3M"
    },
    "enabled": true,
    "id": "fdeef3a6-aa35-4de2-aec6-9072ccd843b2",
    "isAcknowledgementRequired": false,
    "name": "Outbound CIM AIIDA data with 2 seconds resolution",
    "policyLink": "https://example.com/toc",
    "purpose": "purpose",
    "schemas": [
      "SMART-METER-P1-CIM-V1-04"
    ],
    "transmissionSchedule": "*/2 * * * * *"
  }
}
```

### 3. AIIDA → EDDIE: Respond to Permission Request

AIIDA responds with one of `UNFULFILLABLE`, `ACCEPT`, or `REJECT`.

#### Request

```
PATCH {handshakeUrl}
Authorization: Bearer {accessToken}
```

```json
{
  "operation": "ACCEPT",
  "aiidaId": "123e4567-e89b-12d3-a456-426614174000"
}
```

#### Response (ACCEPT)

```json
{
  "serverUri": "tcp://online.eddie.energy:1883",
  "username": "09520290-ea60-4422-a54c-db8fde93cfd5",
  "password": "z=1CfV%O2jHX03ar8]ev&;W`",
  "dataTopic": "aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/data/outbound",
  "statusTopic": "aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/status",
  "terminationTopic": "aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/termination",
  "acknowledgementTopic": null
}
```

### 4. AIIDA ↔ EDDIE: Data Streaming

- Inbound (**MQTT topic:** `aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/inbound`)
  - `OPAQUE`
  - `MIN_MAX_ENVELOPE_CIM_V1_12`
- Outbound (**MQTT topic:** `aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/outbound`)
  - `SMART_METER_P1_RAW`
  - `SMART_METER_P1_CIM_V1_04`
  - `SMART_METER_P1_CIM_V1_12`

#### Optionally - EDDIE → AIIDA: Acknowledgement

**MQTT topic:** `aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/acknowledgement`

Payload: `ACKNOWLEDGEMENT_CIM_V1_12`

### Optionally - EDDIE → AIIDA: Termination

**MQTT topic:** `aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/termination`

```
09520290-ea60-4422-a54c-db8fde93cfd5
```

### 5. AIIDA → EDDIE: Status Update

AIIDA reports one of `FULFILLED`, `REVOKE`, or `TERMINATED` on the status topic.

**MQTT topic:** `aiida/v1/09520290-ea60-4422-a54c-db8fde93cfd5/status`

```json
{
  "eddie_id": "56faf485-ed0a-4066-8cce-dc227c1350a9",
  "connection_id": "b77f7f5c-3d1b-4597-848f-2001700c228b",
  "data_need_id": "fdeef3a6-aa35-4de2-aec6-9072ccd843b2",
  "permission_id": "09520290-ea60-4422-a54c-db8fde93cfd5",
  "timestamp": "2026-04-28T12:00:00Z",
  "status": "REVOKED"
}
```

```mermaid
---
title: AIIDA Permission Process Model
---
stateDiagram-v2
  direction TB
  state validation_fork <<choice>>
  state permission_fork <<choice>>
  state accepted_fork <<choice>>
  state waiting_for_start_fork <<choice>>
  state streaming_fork <<choice>>
  state join_state_all <<fork>>

  [*] --> CREATED: Create Permission in AIIDA
  CREATED --> FETCHED_DETAILS: Fetch Permission Details from EDDIE
  FETCHED_DETAILS --> validation_fork: Validate
  validation_fork --> permission_fork: Fulfillable
  validation_fork --> UNFULFILLABLE: Not fulfillable
  UNFULFILLABLE --> [*]
  permission_fork --> ACCEPTED: Accept Permission
  permission_fork --> REJECTED: Reject Permission
  REJECTED --> [*]
  ACCEPTED --> accepted_fork: Start Streaming Immediately?
  accepted_fork --> waiting_for_start_fork: Yes
  accepted_fork --> WAITING_FOR_START: No
  WAITING_FOR_START --> waiting_for_start_fork: Start Time Reached
  waiting_for_start_fork --> STREAMING_DATA: Start Streaming
  waiting_for_start_fork --> FAILED_TO_START: Failed to Start
  FAILED_TO_START --> [*]
  STREAMING_DATA --> streaming_fork
  streaming_fork --> REVOKED: Revoked by User
  streaming_fork --> TERMINATED: Terminated by EDDIE
  streaming_fork --> FULFILLED: Permission Expired
  REVOKED --> join_state_all
  TERMINATED --> join_state_all
  FULFILLED --> join_state_all
  join_state_all --> [*]
```
