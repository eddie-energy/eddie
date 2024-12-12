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
You should configure the datasource in the AIIDA settings.

Example of a working configuration:

```yaml
aiida:
  datasources:
    sga:
        - enabled: true
          id: 1
          mqtt-server-uri: tcp://localhost:1884
          mqtt-subscribe-topic: sga/metering
          mqtt-username: sga
          mqtt-password: sga
```
When AIIDA started via docker:

```text
AIIDA_DATASOURCES_SGA_0_ENABLED=true
AIIDA_DATASOURCES_SGA_0_ID=1
AIIDA_DATASOURCES_SGA_0_MQTT_SERVER_URI=tcp://localhost:1884
AIIDA_DATASOURCES_SGA_0_MQTT_SUBSCRIBE_TOPIC=sga/metering
AIIDA_DATASOURCES_SGA_0_MQTT_USERNAME=sga
AIIDA_DATASOURCES_SGA_0_MQTT_PASSWORD=sga
```

Now configure the MQTT settings in the adapter. The values are based on the example working configuration above.
```text
MQTT Server: host or ip of the AIIDA broker
MQTT Port: 1884
MQTT Username: sga
MQTT Password: sga
MQTT Prefix: sga/metering
```
