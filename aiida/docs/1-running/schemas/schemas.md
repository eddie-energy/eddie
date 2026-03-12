# Schemas

Schemas are defined as message formatters that transform the raw energy data retrieved by AIIDA into a specified message format.
These messages are then used to share the energy data with EDDIE in a standardized manner.

Schemas play a crucial role when requesting data from AIIDA, since each country has its own data format.
AIIDA uses the information from each [data source](../data-sources/data-sources.md) to transform its data into a standardized message.

## Outbound Data

Currently, the following schemas are supported for outbound data:

- [Raw message format](raw/raw.md)
    - `SMART-METER-P1-RAW`
- [CIM message format](cim/cim.md)
    - `SMART-METER-P1-CIM-V1-04`
    - `SMART-METER-P1-CIM-V1-12`

## Inbound Data

More information about inbound data can be found in the [inbound data source documentation](../data-sources/mqtt/inbound/inbound-data-source.md).
Currently, the following schemas are supported for inbound data:

- `MIN_MAX_ENVELOPE_CIM_V1_12`: See [this documentation](https://architecture.eddie.energy/framework/2-integrating/messages/cim/min-max-envelope.html)
- `OPAQUE`: See [this documentation](https://architecture.eddie.energy/framework/2-integrating/messages/agnostic.html#opaque-envelopes)