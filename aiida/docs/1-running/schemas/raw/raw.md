# Raw Messages

The Raw messages in AIIDA are used to exchange data in a predefined format without changing the data much.
The data is mapped to an internal `AiidaRecord` class which stores the timestamp when the message is received,
its asset type, the user id, the data source id which provided the data and the actual data represent as a list of the
`AiidaRecordValue` class. The raw messages are sent when the permission contains the schema - `SMART-METER-P1-RAW`.

## Single Data Point

A single data point is represented as an AIIDA Record Value. It contains the following fields:

- `rawTag`: Represents the raw identifier of the given data point this can be either the OBIS code, name or an id.
- `rawValue`: Contains the actual value of the data point as received from the data source.
- `rawUnitOfMeasurement`: It represents the unit of measurement for the given
  `rawValue`. E.g.: this can be `kWh` (kilo watt hour).
- `dataTag`: This is our internal identifier, the OBIS code, for the given data point. This is mapped from the
  `rawTag` using
  the data source configuration.
- `value`: It is the raw value adapted to the unit of measurement for the respective OBIS code.
- `unitOfMeasurement`: Our internal unit of measurement for the respective OBIS Code. E.g. the data can be
  converted from `Wh` to `kWh` or from `W` to `kW`.

## DataTag - OBIS Code

Currently, the following data tags (OBIS codes) are supported:

| EnumValue                                         | Tag        | Unit of Measurement            | Description                         |
|---------------------------------------------------|------------|--------------------------------|-------------------------------------|
| POSITIVE_ACTIVE_ENERGY                            | 1-0:1.8.0  | KILO_WATT_HOUR                 | Imported active energy (total)      |
| POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1                | 1-0:21.8.0 | KILO_WATT_HOUR                 | Imported active energy L1           |
| POSITIVE_ACTIVE_ENERGY_IN_PHASE_L2                | 1-0:41.8.0 | KILO_WATT_HOUR                 | Imported active energy L2           |
| POSITIVE_ACTIVE_ENERGY_IN_PHASE_L3                | 1-0:61.8.0 | KILO_WATT_HOUR                 | Imported active energy L3           |
| NEGATIVE_ACTIVE_ENERGY                            | 1-0:2.8.0  | KILO_WATT_HOUR                 | Exported active energy (total)      |
| NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1                | 1-0:22.8.0 | KILO_WATT_HOUR                 | Exported active energy L1           |
| NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L2                | 1-0:42.8.0 | KILO_WATT_HOUR                 | Exported active energy L2           |
| NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L3                | 1-0:62.8.0 | KILO_WATT_HOUR                 | Exported active energy L3           |
| POSITIVE_ACTIVE_INSTANTANEOUS_POWER               | 1-0:1.7.0  | KILO_WATT                      | Instant active power import (total) |
| POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1   | 1-0:21.7.0 | KILO_WATT                      | Instant active power import L1      |
| POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2   | 1-0:41.7.0 | KILO_WATT                      | Instant active power import L2      |
| POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3   | 1-0:61.7.0 | KILO_WATT                      | Instant active power import L3      |
| NEGATIVE_ACTIVE_INSTANTANEOUS_POWER               | 1-0:2.7.0  | KILO_WATT                      | Instant active power export (total) |
| POSITIVE_REACTIVE_INSTANTANEOUS_POWER             | 1-0:3.7.0  | KILO_VOLT_AMPERE_REACTIVE      | Instant reactive power import       |
| POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1 | 1-0:23.7.0 | KILO_VOLT_AMPERE_REACTIVE      | Instant reactive power import L1    |
| POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2 | 1-0:43.7.0 | KILO_VOLT_AMPERE_REACTIVE      | Instant reactive power import L2    |
| POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3 | 1-0:63.7.0 | KILO_VOLT_AMPERE_REACTIVE      | Instant reactive power import L3    |
| NEGATIVE_REACTIVE_INSTANTANEOUS_POWER             | 1-0:4.7.0  | KILO_VOLT_AMPERE_REACTIVE      | Instant reactive power export       |
| POSITIVE_REACTIVE_ENERGY_IN_TARIFF                | 1-0:3.8.1  | KILO_VOLT_AMPERE_REACTIVE_HOUR | Imported reactive energy (T1)       |
| NEGATIVE_REACTIVE_ENERGY_IN_TARIFF                | 1-0:4.8.1  | KILO_VOLT_AMPERE_REACTIVE_HOUR | Exported reactive energy (T1)       |
| MAXIMUM_CURRENT                                   | 1-0:11.6.0 | AMPERE                         | Max current (since reset)           |
| MAXIMUM_CURRENT_IN_PHASE_L1                       | 1-0:31.6.0 | AMPERE                         | Max current L1                      |
| MAXIMUM_CURRENT_IN_PHASE_L2                       | 1-0:51.6.0 | AMPERE                         | Max current L2                      |
| MAXIMUM_CURRENT_IN_PHASE_L3                       | 1-0:71.6.0 | AMPERE                         | Max current L3                      |
| APPARENT_INSTANTANEOUS_POWER                      | 1-0:9.7.0  | KILO_VOLT_AMPERE               | Instant apparent power (total)      |
| INSTANTANEOUS_CURRENT                             | 1-0:11.7.0 | AMPERE                         | Instant current (total)             |
| INSTANTANEOUS_CURRENT_IN_PHASE_L1                 | 1-0:31.7.0 | AMPERE                         | Instant current L1                  |
| INSTANTANEOUS_CURRENT_IN_PHASE_L2                 | 1-0:51.7.0 | AMPERE                         | Instant current L2                  |
| INSTANTANEOUS_CURRENT_IN_PHASE_L3                 | 1-0:71.7.0 | AMPERE                         | Instant current L3                  |
| INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL            | 1-0:91.7.0 | AMPERE                         | Instant current N                   |
| INSTANTANEOUS_POWER_FACTOR                        | 1-0:13.7.0 | NONE                           | Power factor (total)                |
| INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L1            | 1-0:33.7.0 | NONE                           | Power factor L1                     |
| INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L2            | 1-0:53.7.0 | NONE                           | Power factor L2                     |
| INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L3            | 1-0:73.7.0 | NONE                           | Power factor L3                     |
| INSTANTANEOUS_VOLTAGE                             | 1-0:12.7.0 | VOLT                           | Instant voltage (total)             |
| INSTANTANEOUS_VOLTAGE_IN_PHASE_L1                 | 1-0:32.7.0 | VOLT                           | Instant voltage L1                  |
| INSTANTANEOUS_VOLTAGE_IN_PHASE_L2                 | 1-0:52.7.0 | VOLT                           | Instant voltage L2                  |
| INSTANTANEOUS_VOLTAGE_IN_PHASE_L3                 | 1-0:72.7.0 | VOLT                           | Instant voltage L3                  |
| FREQUENCY                                         | 1-0:14.7.0 | HERTZ                          | Grid frequency                      |
| DEVICE_ID_1                                       | 0-0:96.1.0 | NONE                           | Device ID                           |
| TIME                                              | 0-0:1.0.0  | NONE                           | Device time                         |
| UPTIME                                            | 0-0:2.0.0  | NONE                           | Device uptime                       |
| UNKNOWN                                           | 0-0:0.0.0  | UNKNOWN                        | Unknown/unsupported code            |
| METER_SERIAL                                      | 0-0:C.1.0  | NONE                           | Meter serial number                 |

## Units of Measurement

Currently, the following units are supported:

| EnumValue                      | Symbol  |
|--------------------------------|---------|
| WATT                           | W       |
| KILO_WATT                      | kW      |
| WATT_HOUR                      | Wh      |
| KILO_WATT_HOUR                 | kWh     |
| VOLT_AMPERE_REACTIVE           | VAr     |
| KILO_VOLT_AMPERE_REACTIVE      | kVAr    |
| VOLT_AMPERE_REACTIVE_HOUR      | VArh    |
| KILO_VOLT_AMPERE_REACTIVE_HOUR | kVArh   |
| AMPERE                         | A       |
| VOLT                           | V       |
| VOLT_AMPERE                    | VA      |
| KILO_VOLT_AMPERE               | kVA     |
| HERTZ                          | Hz      |
| NONE                           | none    |
| UNKNOWN                        | unknown |

## Example Raw Message

```json
{
  "timestamp": "2025-10-08T07:40:38.711Z",
  "asset": "CONNECTION-AGREEMENT-POINT",
  "userId": "138cace8-ab2a-439f-a878-1e0aa06ee214",
  "dataSourceId": "210e100f-7d5d-49d4-930d-8769672011db",
  "values": [
    {
      "rawTag": "1-0:1.7.0",
      "dataTag": "1-0:1.7.0",
      "rawValue": "995",
      "rawUnitOfMeasurement": "kW",
      "value": "995",
      "unitOfMeasurement": "kW",
    },
    {
      "rawTag": "1-0:1.8.0",
      "dataTag": "1-0:1.8.0",
      "rawValue": "1348",
      "rawUnitOfMeasurement": "kWh",
      "value": "1348",
      "unitOfMeasurement": "kWh",
    }
  ]
}
```