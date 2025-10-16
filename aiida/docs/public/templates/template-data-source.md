# Data Source: XYZ (Country)
> [Data Sources](../../1-running/datasources/data-sources.md) / [MQTT-based](../../1-running/datasources/mqtt/mqtt-data-sources.md)

Briefly describe the data source, its purpose, and what kind of data it provides.

Optionally include an image or diagram.

## Integration with AIIDA

### Data Source Configuration

Explain how to configure the **data source device or system itself** before connecting it to AIIDA.

- Available modes (e.g., local MQTT, cloud MQTT, Modbus, etc.)
- Configuration interface (web UI, app, command line, …)
- Required credentials or activation keys
- Example screenshots or configuration snippets

### Setup in AIIDA

Describe what must be done within AIIDA to enable the data source.

- Required environment variables (.env file)
- Configuration options in the AIIDA web interface

### Connect with AIIDA

Document the connection details between the data source and AIIDA.

- MQTT topics and topic structure
- Authentication details (username, password, tokens)
- Broker URLs and ports
- Example `curl` or `mosquitto_sub` commands
- How to verify successful connection (logs, dashboards, etc.)

## Optional: Additional things to consider

Include any extra information, best practices, or pitfalls, such as:

- Data update frequency or payload format
- Known issues or compatibility notes
- Recommended monitoring or troubleshooting steps
- Device-specific constraints (firmware versions, etc.)

## Sources

List all relevant documentation or external resources used:

- Official vendor documentation
- MQTT topic reference
- AIIDA integration guide