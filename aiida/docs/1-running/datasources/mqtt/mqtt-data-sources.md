# MQTT-based Data Sources

The MQTT data sources share the logic of connecting to an MQTT broker, authenticating and subscribing to a topic.
The following MQTT data sources are supported:

- Österreichs Energie
- Smart Gateways
- Micro Teleinfo V3
- CIM
- [Inbound](inbound/inbound-data-source.md)
- [Sinapsi Alfa](it/sinapsi-alfa-data-source.md)
- [Shelly](shelly/shelly-data-source.md)

#### Common Properties

- `internalHost`: URL (with protocol and port) of the broker reachable by AIIDA itself (e.g. `tcp://broker:1883`).
- `externalHost`: URL (with protocol and port) of the broker reachable by external devices (e.g. `tcp://mqtt.example.com:1883`).
- `username`: Username for authentication with the broker.
- `password`: Password for authentication with the broker.
- `subscribeTopic`: MQTT topic where the data source should send its data and AIIDA subscribes to it (e.g. `aiida/34982f9e-d5c4-41f3-ae5f-0b3185d586ef/data`).
- `action`: Which actions are allowed for this data source (e.g. `PUBLISH`, `SUBSCRIBE`, or `ALL`).
- `aclType`: Type of access control list (ACL) applied to the data source (e.g. `ALLOW`, or `DENY`).