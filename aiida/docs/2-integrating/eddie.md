# EDDIE

[//]: # (TODO: add more details - this is just a placeholder for now)

## MQTT

EDDIE running at the EP uses **MQTT** as the communication protocol between the EP and AIIDA.

### ACLs

TBD

### Topics

- **Outbound**: AIIDA publishes to `aiida/v1/{PERMISSION_ID}/data/outbound`
- **Inbound**: AIIDA subscribes to `aiida/v1/{PERMISSION_ID}/data/inbound`