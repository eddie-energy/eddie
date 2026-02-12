# Real Time Data Market Document

The Real Time Data (RTD) Market Document contains near-real time (NRT) data that is received from a data source in AIIDA.
The RTD Market Document is sent as JSON document over MQTT from AIIDA to EDDIE.

The XSD files for the different versions of this document can be found here:
- [v1.04](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_04/rtd)
- [v1.12](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/rtd)

```xml
<MarketDocument>
    <!-- The UUID of the Real Time Data Market Document -->
    <mRID>bff481d5-edd8-4602-9c54-838b386ab4dd</mRID>
    <!-- UTC timestamp when the Market Document was created -->
    <createdDateTime>2025-07-01T09:44:00.00032307Z</createdDateTime>
    <TimeSeries>
        <!-- The values of an AIIDA Record -->
        <TimeSeries>
            <!-- The version of the Time Series, currently always 1.0 -->
            <version>1.0</version>
            <registeredResource.mRID codingScheme="NAT">
                <!-- The UUID of the data source which provided the data -->
                <value>5eef407d-d14f-49d4-b61a-769a20caa540</value>
            </registeredResource.mRID>
            <!-- UTC timestamp of the AIIDA Record -->
            <dateAndOrTime.dateTime>2025-07-01T07:43:59.073747585Z</dateAndOrTime.dateTime>
            <Quantity>
                <!-- Equivalents with an AIIDA Record Value -->
                <Quantity>
                    <!-- The quantity value of the AIIDA record -->
                    <quantity>25</quantity>
                    <!-- The type of the quantity -->
                    <type>0</type>
                    <!-- The quality of the quantity -->
                    <quality>AS_PROVIDED</quality>
                </Quantity>
                <!-- Equivalents with an AIIDA Record Value -->
                <Quantity>
                    <!-- The quantity value of the AIIDA record -->
                    <quantity>1750</quantity>
                    <!-- The type of the quantity -->
                    <type>2</type>
                    <!-- The quality of the quantity -->
                    <quality>AS_PROVIDED</quality>
                </Quantity>
            </Quantity>
        </TimeSeries>
    </TimeSeries>
</MarketDocument>
```

## Time Series

Time Series in the RTD Market Document represent the values of an AIIDA Record, called quantities in the CIM.
Quantities have a type that indicates what the quantity represents as well as its unit.
Quantities also provide a quality field that indicates the quality of the quantity.

### Type

Currently, the following types are supported:

| EnumValue | EnumName                                                    | Description                                                                 | Unit |
|-----------|-------------------------------------------------------------|-----------------------------------------------------------------------------|------|
| `0`       | `TOTAL_ACTIVE_ENERGY_CONSUMED_KWH`                          | Cumulative amount of electrical energy consumed from the grid               | kWh  |
| `1`       | `TOTAL_ACTIVE_ENERGY_PRODUCED_KWH`                          | Cumulative amount of electrical energy exported back to the grid            | kWh  |
| `2`       | `INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW`                 | Real-time power currently being consumed from the grid                      | kW   |
| `3`       | `INSTANTANEOUS_ACTIVE_POWER_GENERATION_KW`                  | Real-time power currently being generated and exported to the grid          | kW   |
| `4`       | `INSTANTANEOUS_VOLTAGE_V_IN_PHASE_L1`                       | Real-time voltage level on phase L1                                         | V    |
| `5`       | `INSTANTANEOUS_VOLTAGE_V_IN_PHASE_L2`                       | Real-time voltage level on phase L2                                         | V    |
| `6`       | `INSTANTANEOUS_VOLTAGE_V_IN_PHASE_L3`                       | Real-time voltage level on phase L3                                         | V    |
| `7`       | `INSTANTANEOUS_CURRENT_A_IN_PHASE_L1`                       | Real-time current flowing on phase L1                                       | A    |
| `8`       | `INSTANTANEOUS_CURRENT_A_IN_PHASE_L2`                       | Real-time current flowing on phase L2                                       | A    |
| `9`       | `INSTANTANEOUS_CURRENT_A_IN_PHASE_L3`                       | Real-time current flowing on phase L3                                       | A    |
| `10`      | `INSTANTANEOUS_POWERFACTOR`                                 | Ratio of real power to apparent power, indicating efficiency of power usage | -    |
| `11`      | `TOTAL_ACTIVE_ENERGY_CONSUMED_KWH_IN_PHASE_L1`              | Amount of electrical energy consumed from the grid on phase L1              | kWh  |
| `12`      | `TOTAL_ACTIVE_ENERGY_CONSUMED_KWH_IN_PHASE_L2`              | Amount of electrical energy consumed from the grid on phase L2              | kWh  |
| `13`      | `TOTAL_ACTIVE_ENERGY_CONSUMED_KWH_IN_PHASE_L3`              | Amount of electrical energy consumed from the grid on phase L3              | kWh  |
| `14`      | `TOTAL_ACTIVE_ENERGY_PRODUCED_KWH_IN_PHASE_L1`              | Amount of electrical energy exported back to the grid on phase L1           | kWh  |
| `15`      | `TOTAL_ACTIVE_ENERGY_PRODUCED_KWH_IN_PHASE_L2`              | Amount of electrical energy exported back to the grid on phase L2           | kWh  |
| `16`      | `TOTAL_ACTIVE_ENERGY_PRODUCED_KWH_IN_PHASE_L3`              | Amount of electrical energy exported back to the grid on phase L3           | kWh  |
| `17`      | `INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW_IN_PHASE_L1`     | Real-time power currently being consumed from the grid on phase L1          | kW   |
| `18`      | `INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW_IN_PHASE_L2`     | Real-time power currently being consumed from the grid on phase L2          | kW   |
| `19`      | `INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW_IN_PHASE_L3`     | Real-time power currently being consumed from the grid on phase L3          | kW   |
| `20`      | `INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR`             | Real-time reactive power currently being consumed from the grid on phase L1 | kvar |
| `21`      | `INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR_IN_PHASE_L1` | Real-time reactive power currently being consumed from the grid on phase L1 | kvar |
| `22`      | `INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR_IN_PHASE_L2` | Real-time reactive power currently being consumed from the grid on phase L2 | kvar |
| `23`      | `INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR_IN_PHASE_L3` | Real-time reactive power currently being consumed from the grid on phase L3 | kvar |
| `24`      | `INSTANTANEOUS_REACTIVE_POWER_GENERATION_KVAR`              | Real-time reactive power currently being generated and exported to the grid | kvar |
| `25`      | `INSTANTANEOUS_VOLTAGE_V`                                   | Real-time voltage level                                                     | V    |
| `26`      | `INSTANTANEOUS_CURRENT_A`                                   | Real-time current flowing                                                   | A    |
| `27`      | `INSTANTANEOUS_CURRENT_A_IN_PHASE_NEUTRAL`                  | Real-time current flowing on the neutral conductor                          | A    |
| `28`      | `MAXIMUM_CURRENT_A`                                         | Maximum current observed during the time interval                           | A    |
| `29`      | `MAXIMUM_CURRENT_A_IN_PHASE_L1`                             | Maximum current observed during the time interval on phase L1               | A    |
| `30`      | `MAXIMUM_CURRENT_A_IN_PHASE_L2`                             | Maximum current observed during the time interval on phase L2               | A    |
| `31`      | `MAXIMUM_CURRENT_A_IN_PHASE_L3`                             | Maximum current observed during the time interval on phase L3               | A    |
| `32`      | `INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L1`                    | Real-time power factor on phase L1                                          | -    |
| `33`      | `INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L2`                    | Real-time power factor on phase L2                                          | -    |
| `34`      | `INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L3`                    | Real-time power factor on phase L3                                          | -    |
| `35`      | `FREQUENCY_HZ`                                              | Real-time frequency of the electrical system                                | Hz   |

### Quality

Currently, the following qualities are supported:

| EnumValue | EnumName        | Description                                        |
|-----------|-----------------|----------------------------------------------------|
| `A01`     | `ADJUSTED`      | The value has been adjusted                        |
| `A02`     | `NOT_AVAILABLE` | The value is not available                         |
| `A03`     | `ESTIMATED`     | The value is estimated                             |
| `A04`     | `AS_PROVIDED`   | The value is provided as is, without modifications |
| `A05`     | `INCOMPLETE`    | The value is incomplete                            |
| `A06`     | `CALCULATED`    | The value has been calculated                      |

## Complete JSON Example

```json
{
  "MessageDocumentHeader": {
    "creationDateTime": "2026-02-11T15:32:25Z",
    "MetaInformation": {
      "connectionId": "1",
      "requestPermissionId": "70744400-a059-4fc8-ab36-d68b2bb877e1",
      "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
      "documentType": "near-real-time-market-document",
      "finalCustomerId": "88e0fc2c-4ea7-4850-a736-8b9742757518",
      "dataSourceId": "7d2b2547-27dd-4fe0-9516-707540e1184f",
      "regionConnector": "aiida",
      "regionCountry": "AT",
      "Asset": {
        "type": "CONNECTION-AGREEMENT-POINT",
        "operatorId": "AT003000",
        "meterId": "003114735"
      }
    }
  },
  "MarketDocument": {
    "mRID": "bfc16eda-4f05-4711-b319-af17ec0ce6d5",
    "createdDateTime": "2026-02-11T15:32:25Z",
    "TimeSeries": [
      {
        "version": "1.0",
        "dateAndOrTime.dateTime": "2026-02-11T15:32:24Z",
        "Quantity": [
          {
            "quantity": 0.132,
            "type": "2",
            "quality": "AS_PROVIDED"
          },
          {
            "quantity": 65238.377,
            "type": "0",
            "quality": "AS_PROVIDED"
          }
        ],
        "registeredResource.mRID": {
          "value": "7d2b2547-27dd-4fe0-9516-707540e1184f",
          "codingScheme": "NAT"
        }
      }
    ]
  }
}
```