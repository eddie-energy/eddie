# Real Time Data Market Document

The Real Time Data (RTD) Market Document contains near-real time (NRT) data that is received from a data source in AIIDA.
The RTD Market Document is sent as JSON document over MQTT from AIIDA to EDDIE.
The XSD file for this document can be found [here](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_04/rtd).

```xml

<marketDocument>
    <!-- The UUID of the Real Time Data Market Document -->
    <mrid>bff481d5-edd8-4602-9c54-838b386ab4dd</mrid>
    <!-- UTC timestamp when the Market Document was created -->
    <createdDateTime>2025-07-01T09:44:00.00032307Z</createdDateTime>
    <!-- The values of an AIIDA Record -->
    <timeSeries>
        <!-- The version of the Time Series, currrently always 1.0 -->
        <version>1.0</version>
        <registeredResourceMRID>
            <!-- The UUID of the data source which provided the data -->
            <value>5eef407d-d14f-49d4-b61a-769a20caa540</value>
            <!-- The coding is based on the country the data source is located at -->
            <codingScheme>NAT</codingScheme>
        </registeredResourceMRID>
        <!-- UTC timestamp of the AIIDA Record -->
        <dateAndOrTimeDateTime>2025-07-01T07:43:59.073747585Z</dateAndOrTimeDateTime>
        <!-- Equivalents with an AIIDA Record Value -->
        <quantities>
            <!-- The quantity value of the AIIDA record -->
            <quantity>25</quantity>
            <!-- The type of the quantity -->
            <type>0</type>
            <!-- The quality of the quantity -->
            <quality>AS_PROVIDED</quality>
        </quantities>
        <!-- Equivalents with an AIIDA Record Value -->
        <quantities>
            <!-- The quantity value of the AIIDA record -->
            <quantity>1750</quantity>
            <!-- The type of the quantity -->
            <type>2</type>
            <!-- The quality of the quantity -->
            <quality>AS_PROVIDED</quality>
        </quantities>
    </timeSeries>
</marketDocument>
```

## Time Series

Time Series in the RTD Market Document represent the values of an AIIDA Record, called quantities in the CIM.
Quantities have a type that indicates what the quantity represents as well as its unit.
Quantities also provide a quality field that indicates the quality of the quantity.

### Type

Currently, the following types are supported:

| EnumValue | EnumName                                      | Description                                                                 | Unit |
|-----------|-----------------------------------------------|-----------------------------------------------------------------------------|------|
| 0         | TotalActiveEnergyConsumed_import_kWh          | Cumulative amount of electrical energy consumed from the grid               | kWh  |
| 1         | TotalActiveEnergyProduced_export_kWh          | Cumulative amount of electrical energy exported back to the grid            | kWh  |
| 2         | InstantaneousActivePowerConsumption_import_kW | Real-time power currently being consumed from the grid                      | kW   |
| 3         | InstantaneousActivePowerGeneration_export_kW  | Real-time power currently being generated and exported to the grid          | kW   |
| 4         | InstantaneousVoltage_V_on_phaseL1             | Real-time voltage level on phase L1                                         | V    |
| 5         | InstantaneousVoltage_V_on_phaseL2             | Real-time voltage level on phase L2                                         | V    |
| 6         | InstantaneousVoltage_V_on_phaseL3             | Real-time voltage level on phase L3                                         | V    |
| 7         | InstantaneousCurrent_A_on_phaseL1             | Real-time current flowing on phase L1                                       | A    |
| 8         | InstantaneousCurrent_A_on_phaseL2             | Real-time current flowing on phase L2                                       | A    |
| 9         | InstantaneousCurrent_A_on_phaseL3             | Real-time current flowing on phase L3                                       | A    |
| 10        | PowerFactor                                   | Ratio of real power to apparent power, indicating efficiency of power usage | -    |

### Quality

Currently, the following qualities are supported:

| EnumValue | EnumName      | Description                                        |
|-----------|---------------|----------------------------------------------------|
| A01       | ADJUSTED      | The value has been adjusted                        |
| A02       | NOT_AVAILABLE | The value is not available                         |
| A03       | ESTIMATED     | The value is estimated                             |
| A04       | AS_PROVIDED   | The value is provided as is, without modifications |
| A05       | INCOMPLETE    | The value is incomplete                            |
| A06       | CALCULATED    | The value has been calculated                      |

## Complete JSON Example

```json
{
  "messageDocumentHeader.creationDateTime": "2025-07-01T09:44:00.00040249Z",
  "messageDocumentHeader.metaInformation.connectionId": "3",
  "messageDocumentHeader.metaInformation.dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
  "messageDocumentHeader.metaInformation.documentType": "near-real-time-market-document",
  "messageDocumentHeader.metaInformation.permissionId": "150cfd97-64bb-402b-838f-57f8605713b7",
  "messageDocumentHeader.metaInformation.finalCustomerId": "008cf1d6-e118-45a8-bc17-a331dfc57e77",
  "messageDocumentHeader.metaInformation.asset": "CONNECTION-AGREEMENT-POINT",
  "messageDocumentHeader.metaInformation.dataSourceId": "5eef407d-d14f-49d4-b61a-769a20caa540",
  "messageDocumentHeader.metaInformation.regionConnector": "aiida",
  "messageDocumentHeader.metaInformation.regionCountry": "AT",
  "marketDocument": {
    "mrid": "bff481d5-edd8-4602-9c54-838b386ab4dd",
    "createdDateTime": "2025-07-01T09:44:00.00032307Z",
    "timeSeries": [
      {
        "version": "1.0",
        "registeredResourceMRID": {
          "value": "5eef407d-d14f-49d4-b61a-769a20caa540",
          "codingScheme": "NAT"
        },
        "dateAndOrTimeDateTime": "2025-07-01T07:43:59.073747585Z",
        "quantities": [
          {
            "quantity": 25,
            "type": "0",
            "quality": "AS_PROVIDED"
          },
          {
            "quantity": 1750,
            "type": "2",
            "quality": "AS_PROVIDED"
          }
        ]
      }
    ]
  }
}
```