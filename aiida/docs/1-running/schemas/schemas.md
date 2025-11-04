# Schemas

Schemas are defined as message formatters that transform the raw energy data retrieved by AIIDA into a specified message format.
These messages are then used to share the energy data with EDDIE in a standardized manner.

Schemas play a crucial role when requesting data from AIIDA, since each country has its own data format.
AIIDA uses the information from each [data source](../data-sources/data-sources.md) to transform its data into a standardized message.

Currently, the following schemas are supported:

- [Raw message format](raw/raw.md)
- [CIM message format](cim/cim.md)