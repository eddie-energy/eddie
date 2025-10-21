# Database

AIIDA uses a [TimescaleDB](https://github.com/timescale/timescaledb) as its primary database.
It was chosen because it is a relational database (PostgreSQL) optimized for time-series data.

![](../../images/database/database-data-model.png)

The figure above shows the relational data model of AIIDA's database.
On the first start of the AIIDA application, a UUID is generated and stored in the `aiida_application_information`
table.

AIIDA stores data sources and their configuration in the `data_source` table when a user adds one.
After adding a data source, AIIDA starts receiving data from it and stores the data received in the `aiida_record`
table.

Each `aiida_record` may contain multiple measurements, which are stored in the `aiida_record_value` table.
When a user creates a permission for an Eligible Party (EP) to access their data, a new entry is created in the
`permission` table. This permission is always associated with one data source.

For each permission, the given data need is stored locally as a copy in the `aiida_local_data_need` table.
If the data need is MQTT-based, the MQTT configuration is stored in the `mqtt_streaming_config` table.

AIIDA's EMQX MQTT broker uses the `data_source` table to authenticate and authorize data sources when they connect to
the broker.
For each data source, a user is created in the `data_source` table with a username and password.
Additionally, a dedicated topic is created for each data source.
This user is both used by the virtual data source created by AIIDA and the physical data source (e.g., a smart meter or
IoT device) to subscribe and publish to this topic.

[Inbound data sources](../../1-running/data-sources/mqtt/inbound/inbound-data-source.md) allow an EP to send data to AIIDA.
The incoming payload is stored in the `inbound_record` table.