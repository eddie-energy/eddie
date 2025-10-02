# MicroTeleinfoV3

This USB adapter supports the Linky smart meter in France.
It converts the data retrieved to a serial interface.

![Smart Meter Adapter](MicroTeleinfoV3.jpg)

*Image from official [tindie product site](https://www.tindie.com/products/hallard/micro-teleinfo-v30/).*

## Installation

In order to connect with the USB dongle, it has to be both plugged into a system which supports USB and an internet
connection such as a raspberry pi.
On a raspberry pi the MicroTeleinfoV3 device should be located under
`/dev/ttyACM0` as mentioned on the [product's website](https://www.tindie.com/products/hallard/micro-teleinfo-v30/).

Otherwise, both of the following commands can be useful to troubleshoot the mount of the device:

1. `dmesg -e` prints the kernel ring buffer, which also prints if the MicroTeleinfoV3 was found as device.
    1. Output should look something like this:
       ```
       usb 1-1: New USB device found, idVendor=1a86, idProduct=55d4, bcdDevice= 4.43
       usb 1-1: New USB device strings: Mfr=1, Product=2, SerialNumber=3
       usb 1-1: Product: uTinfo-V3.0
       usb 1-1: Manufacturer: CH2i
       usb 1-1: SerialNumber: TINFO-2248
       ```
2. `sudo lsusb -v` prints information about connected USB hardware devices

The USB dongle must now be connected to the Linky smart meter via 2 small cables, like a telephone wire.
As mentioned in the official [GitHub documentation](https://github.com/hallard/uTeleinfo), the cables must then be
connected to the smart meter **I1** and **I2** interface.
Now a blue LED must flash very quickly which indicates a present teleinfo signal.
In order to test this connection refer to the GitHub documentation mentioned above.

## Publish incoming data to MQTT

A simple way to publish the serial data to MQTT and to retrieve it with AIIDA, is the [teleinfo2mqtt image](https://github.com/fmartinou/teleinfo2mqtt).
This image connects itself with MicroTeleinfoV3, sanitizes the retrieved data and publishes the data to a specified
topic in a raw and sanitized format. The topic it publishes to is <mqtt-base-topic>/<electricity-meter-id>, where
<mqtt-base-topic> is **teleinfo** by default.
The <electricity-meter-id> must be evaluated beforehand and has to be added to the datasource configuration.
To see all configuration options of the image please refer to the official [documentation](https://fmartinou.github.io/teleinfo2mqtt/#/configuration/).
The .env-fr-teleinfo file provides the most important configuration values with mostly prefilled valus which can be used.
The example configuration looks as follows:

```text
MQTT_BASE_TOPIC=teleinfo
MQTT_URL=mqtt://REPLACE_ME
MQTT_USER=REPLACE_ME
MQTT_PASSWORD=REPLACE_ME
EMIT_INTERVAL=2
HASS_DISCOVERY=false
SERIAL=/dev/ttyACM0
TIC_MODE=history
TZ=Europe/Paris
```

## How to use with AIIDA

In order to add the MicroTeleinfoV3 as datasource in AIIDA, the properties of the datasource have to be adapted
correctly. The datasource has to be enabled, an ID has to be assigned to it and the broker uri to where the datasource
should connect has to be added. The default topic of the teleinfo image is **teleinfo**, if the `MQTT_BASE_TOPIC` of the
image's configuration has been changed, the value has to be adapted in this configuration too.
Provide the username and password in order to authenticate to the MQTT broker you want to connect to if necessary or leave empty.
Lastly, add the **metering ID** of the Linky smart meter to this configuration.
The image publishes the data by default to the topic`teleinfo/<metering-id>`.
Please do not manually add the metering ID to the mqtt-subscribe-topic property, just provide the metering ID to the dedicated property.

Example of a working configuration:

```yaml
aiida:
  datasources:
    fr:
      teleinfo:
        - enabled: true
          id: 1
          mqtt-server-uri: tcp://localhost:1884
          mqtt-subscribe-topic: teleinfo
          mqtt-username:
          mqtt-password:
          metering-id: 123456789123
```

When AIIDA started via docker:

```text
AIIDA_DATASOURCES_FR_TELEINFO_0_ENABLED=true
AIIDA_DATASOURCES_FR_TELEINFO_0_ID=1
AIIDA_DATASOURCES_FR_TELEINFO_0_MQTT_SERVER_URI=tcp://localhost:1884
AIIDA_DATASOURCES_FR_TELEINFO_0_MQTT_SUBSCRIBE_TOPIC=teleinfo
AIIDA_DATASOURCES_FR_TELEINFO_0_MQTT_USERNAME=
AIIDA_DATASOURCES_FR_TELEINFO_0_MQTT_PASSWORD=
AIIDA_DATASOURCES_FR_TELEINFO_0_METERING_ID=123456789123
```

In order to start the docker compose with the _teleinfo2mqtt_ image use the following command:

```shell
docker compose --profile teleinfo up -d
```