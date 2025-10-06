# Sinapsi Alfa

The Sinapsi Alfa device connects Italian Smart Meters with AIIDA. While it is technically possible to access the data 
locally via Modbus, the requirements for Oetzi Strom rely on AIIDA’s cloud-based setup - therefore, the global MQTT 
broker of Sinapsi Alfa is used.

![Sinapsi Alfa](SinapsiAlfa.jpg)

## Prerequisites

Before connecting, set the following environment variables in your .env file using the provided Sinapsi credentials:

```bash
DATA_SOURCE_IT_SINAPSI_ALFA_MQTT_USERNAME=<your_username>
DATA_SOURCE_IT_SINAPSI_ALFA_MQTT_PASSWORD=<your_password>
```

These credentials are required to authenticate with the MQTT broker and, as far as known, are only available for B2B customers.

> **Note:** Sinapsi Alfa is not available for private customers.

In [MySinapsiB2B](https://app.sghiot.com/mysinapsibtb) the Sinapsi Alfa device must be registered.
For that the activation key must be entered.

## Activation Key

Each Sinapsi Alfa device requires an activation key to connect to the MQTT broker.
This key is a unique identifier found either:

- on the device label, or
- in the Sinapsi Alfa mobile app.

When creating a new data source of type Sinapsi Alfa in the AIIDA UI, you will be prompted to enter this activation key.

## Data Structure

The device publishes its readings as MQTT messages - each containing data per OBIS code.

> **Broker:** `hstbrk.sghiot.com`
> **Topic:** `/oetzi/iomtsgdata/{ACTIVATION_KEY}`

### Example payload

```json
[
  {
    "du": "DU12345678",
    "pod": "IT00123456789A",
    "data": [
      {
        "ts": "1757506202",
        "1-0:1.7.0.255_3,0_2": "59"
      }
    ]
  }
]
```