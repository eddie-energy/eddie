# Smart Gateways Adapter

![Smart Meter Adapter](sga.jpg)

This adapter supports following smart meters:

- Netherlands
- Belgium
- Sweden
- Denmark
- Finland
- Hungary
- Lithuania
- Switzerland

But the current implementation is only tested with the Netherlands smart meter.

## Installation

Place the adapter on the smart meter and connect the power source if needed. The adapter
will create a setup Wi-Fi network with the name `Smart Gateways P1 READER`. Connect
to this network with the password `12345678`. Follow the instructions to connect the
adapter to your home Wi-Fi network. Once connected, the adapter will reboot.

You need to know the IP address of the adapter to connect to it. You can find the IP with tools such as:
- Wifiman (App)
- nmap (command line tool)
- Router interface

The web interface for the adapter can be accessed by entering this url `http://<IP-ADDRESS>:82`
in your browser. The default username is `admin` and the default password is `smartgateways`.

## How to use with AIIDA

The adapter needs to send the smart meter data via MQTT to the AIIDA broker.
You should configure the datasource in the AIIDA UI.

Create a new datasource in AIIDA with the datasource type `Smart Gateways Adapter`.
This will generate the MQTT settings for the adapter.

An example configuration is shown below. The values are based on the example working configuration above.
```text
MQTT Server: host or ip of the AIIDA broker
MQTT Port: 1884
MQTT Username: unique username (e.g.: r0bw57fSZM)
MQTT Password: unique password (e.g.: xfeAHruBB4)
MQTT Topic: AcQY334Z4Q/dsmr/reading/+
```

Now configure the MQTT settings in the adapter. The values are based on the example working configuration above.
```text
MQTT Server: host or ip of the AIIDA broker
MQTT Port: 1884
MQTT Username: unique username (e.g.: r0bw57fSZM)
MQTT Password: unique password (e.g.: xfeAHruBB4)
MQTT Prefix: random string of 10 characters (e.g. AcQY334Z4Q)
```

For the prefix only the first 10 characters of the MQTT Topic are used. In this
example the prefix is `AcQY334Z4Q`.

An example of the MQTT configuration of the adapter is shown below.

![Smart Meter Adapter](sga-mqtt-config.png)