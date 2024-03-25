# OesterreichsEnergieAdapter

The smart meter adapter by Oesterreichs Energie supports all smart meters deployed in Austria, regardless of their
physical interface.

## How to use with AIIDA

1. Get the appropriate smart meter adapter for your smart meter (see
   their [website](https://oesterreichsenergie.at/smart-meter/technische-leitfaeden) for details and where to purchase
   an adapter).
2. Connect the adapter to your smart meter and configure it to publish the data to a MQTT broker. You can use a cloud
   MQTT server although a local one would probably provide better latency and privacy/security if configured correctly.
3. In the AIIDA datasources UI, add the OesterreichsEnergieAdapter datasource and enter the same MQTT broker, and if
   necessary authentication credentials.
4. The data is now available in AIIDA.

![Smart Meter Adapter](OesterreichsEnergieAdapter.png)

*Image from "Datenblatt Smart-Meter-Adapter"
from [Oesterreichs Energie](https://oesterreichsenergie.at/smart-meter/technische-leitfaeden)*