# Validated Historical Data Market Documents

Validated historical data market documents contain metered data that is received from the MDA.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files can be found [here for v0.82](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v0_82/vhd) and [here for v1.04](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_04/vhd).
The following is an example of a validated historical data market document.

::: code-group

```xml [v0.82]
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns1:ValidatedHistoricalData_Envelope xmlns:ns1="http://www.eddie.energy/VHD/EDD01/20240614">
    <ns1:MessageDocumentHeader>
        <!-- Omitted for conciseness -->
    </ns1:MessageDocumentHeader>
    <ns1:ValidatedHistoricalData_MarketDocument>
        <!-- Random UUID identifying this document -->
        <ns1:mRID>183bfc39-22fe-4bc2-bf31-7436b075c6e0</ns1:mRID>
        <!-- The CIM version of the document -->
        <ns1:revisionNumber>0.82</ns1:revisionNumber>
        <!-- Identifies it as a measurement document -->
        <ns1:type>A45</ns1:type>
        <!-- created datetime of this document -->
        <ns1:createdDateTime>2024-12-02T10:28:06Z</ns1:createdDateTime>
        <!-- Sender of the data -->
        <ns1:sender_MarketParticipant.mRID>
            <ns1:codingScheme>A02</ns1:codingScheme>
            <ns1:value>DEMOUTILITY</ns1:value>
        </ns1:sender_MarketParticipant.mRID>
        <!-- Role of the sender, mostly metering point administrators -->
        <ns1:sender_MarketParticipant.marketRole.type>A26</ns1:sender_MarketParticipant.marketRole.type>
        <!-- Receiver of the data -->
        <ns1:receiver_MarketParticipant.mRID>
            <ns1:codingScheme>NAT</ns1:codingScheme>
            <!-- The identifier of the eligible party at the MDA, often client ID or partner ID -->
            <ns1:value>CLIENT_ID</ns1:value>
        </ns1:receiver_MarketParticipant.mRID>
        <!-- Role of the receiver -->
        <ns1:receiver_MarketParticipant.marketRole.type>A13</ns1:receiver_MarketParticipant.marketRole.type>
        <ns1:process.processType>A16</ns1:process.processType>
        <ns1:period.timeInterval>
            <!-- The start of the first measurement -->
            <ns1:start>2024-12-01T00:00Z</ns1:start>
            <!-- The end of the last measurement -->
            <ns1:end>2024-12-02T00:00Z</ns1:end>
        </ns1:period.timeInterval>
        <!-- contains the measurement data of one meter -->
        <ns1:TimeSeriesList>
            <ns1:TimeSeries>
                <!-- Random UUID of this timeseries -->
                <ns1:mRID>4105d729-d044-402d-b76a-2c6076e73bd5</ns1:mRID>
                <!-- Business Type indicates if it is production, consumption or netted-->
                <ns1:businessType>A01</ns1:businessType>
                <!-- Indicates what was consumed/produced/netted -->
                <ns1:product>8716867000016</ns1:product>
                <!-- The flow direction of the energy -->
                <ns1:flowDirection.direction>A01</ns1:flowDirection.direction>
                <!-- Metering Point ID -->
                <ns1:marketEvaluationPoint.mRID>
                    <ns1:codingScheme>A02</ns1:codingScheme>
                    <ns1:value>1788743</ns1:value>
                </ns1:marketEvaluationPoint.mRID>
                <!-- Describes how the values of a measuring period were combined, for example, average over 15 minutes -->
                <ns1:marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation>26</ns1:marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation>
                <!-- How the data was obtained, for example, primary metered and secondary metered -->
                <ns1:marketEvaluationPoint.meterReadings.readings.ReadingType.commodity>1</ns1:marketEvaluationPoint.meterReadings.readings.ReadingType.commodity>
                <!-- Unit of the measurements -->
                <ns1:energy_Measurement_Unit.name>KWH</ns1:energy_Measurement_Unit.name>
                <ns1:Series_PeriodList>
                    <ns1:Series_Period>
                        <ns1:timeInterval>
                            <!-- Start of the series period -->
                            <ns1:start>2024-12-01T00:00Z</ns1:start>
                            <!-- End of the series period -->
                            <ns1:end>2024-12-01T00:15Z</ns1:end>
                        </ns1:timeInterval>
                        <!-- The resolution of the point list following the ISO 8601 duration -->
                        <!-- For the resolutions supported by EDDIE see: https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/Granularity.html -->
                        <ns1:resolution>PT15M</ns1:resolution>
                        <ns1:PointList>
                            <ns1:Point>
                                <!-- Is the index of the Point, the index starts with 1 and is always positive -->
                                <!-- The smallest value is always first, the points should be read in an ascending order -->
                                <ns1:position>1</ns1:position>
                                <!-- Measurement quantity -->
                                <ns1:energy_Quantity.quantity>80000</ns1:energy_Quantity.quantity>
                                <!-- Describes if the value was measured, interpolated, etc -->
                                <ns1:energy_Quantity.quality>A04</ns1:energy_Quantity.quality>
                            </ns1:Point>
                        </ns1:PointList>
                    </ns1:Series_Period>
                </ns1:Series_PeriodList>
                <ns1:ReasonList>
                    <ns1:Reason>
                        <!-- No errors specified, statically set, since it is required by the CIM -->
                        <ns1:code>999</ns1:code>
                    </ns1:Reason>
                </ns1:ReasonList>
            </ns1:TimeSeries>
        </ns1:TimeSeriesList>
    </ns1:ValidatedHistoricalData_MarketDocument>
</ns1:ValidatedHistoricalData_Envelope>
```

```xml [v1.04]
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns1:VHD_Envelope xmlns:ns1="https//eddie.energy/CIM/VHD_v1.04">
    <!-- The datetime when the envelope of the document was created  -->
    <ns1:messageDocumentHeader.creationDateTime>2025-11-25T13:41:59Z</ns1:messageDocumentHeader.creationDateTime>
    <!-- connectionID is given by the eligible party for one or more permission requests -->
    <ns1:messageDocumentHeader.metaInformation.connectionId>1</ns1:messageDocumentHeader.metaInformation.connectionId>
    <!-- the data need ID shows to which data need a permission request is related to, and in turn which data is related to it too -->
    <ns1:messageDocumentHeader.metaInformation.dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns1:messageDocumentHeader.metaInformation.dataNeedId>
    <!-- the document type -->
    <ns1:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns1:messageDocumentHeader.metaInformation.documentType>
    <!-- permissionID uniquely identifies a permission request in EDDIE -->
    <ns1:messageDocumentHeader.metaInformation.permissionId>28bf0826-ad8c-49e0-ba5f-5a50a6795cab</ns1:messageDocumentHeader.metaInformation.permissionId>
    <!-- the region connector ID -->
    <ns1:messageDocumentHeader.metaInformation.region.connector>sim</ns1:messageDocumentHeader.metaInformation.region.connector>
    <!-- the country that the permission request is from -->
    <ns1:messageDocumentHeader.metaInformation.region.country>NDE</ns1:messageDocumentHeader.metaInformation.region.country>
    <ns1:MarketDocument>
        <!-- Random UUID identifying this document -->
        <ns1:mRID>2da6129e-9e04-4fd1-8577-3bdaeaab9ac5</ns1:mRID>
        <!-- The CIM version of the document  without the period -->
        <ns1:revisionNumber>104</ns1:revisionNumber>
        <!-- Identifies it as a measurement document -->
        <ns1:type>A45</ns1:type>
        <!-- created datetime of this document -->
        <ns1:createdDateTime>2025-11-25T13:41:59Z</ns1:createdDateTime>
        <!-- Sender of the data -->
        <ns1:sender_MarketParticipant.mRID codingScheme="A01">sim</ns1:sender_MarketParticipant.mRID>
        <!-- Role of the sender, mostly metering point administrators -->
        <ns1:sender_MarketParticipant.marketRole.type>A26</ns1:sender_MarketParticipant.marketRole.type>
        <!-- Receiver of the data -->
        <!-- The identifier of the eligible party at the MDA, often client ID or partner ID -->
        <ns1:receiver_MarketParticipant.mRID codingScheme="NAT">sim</ns1:receiver_MarketParticipant.mRID>
        <!-- Role of the receiver -->
        <ns1:receiver_MarketParticipant.marketRole.type>A13</ns1:receiver_MarketParticipant.marketRole.type>
        <ns1:period.timeInterval>
            <!-- The start of the first measurement -->
            <ns1:start>2024-12-30T09:49Z</ns1:start>
            <!-- The end of the last measurement -->
            <ns1:end>2024-12-30T10:04Z</ns1:end>
        </ns1:period.timeInterval>
        <!-- Indicates the nature of the process from which the document was generated -->
        <ns1:process.processType>A16</ns1:process.processType>
        <!-- contains the measurement data of one meter -->
        <ns1:TimeSeries>
            <!-- The version of the document, will be always 1 for EDDIE -->
            <ns1:version>1</ns1:version>
            <!-- Random UUID of this timeseries -->
            <ns1:mRID>70210ee2-cced-4433-beb0-dc535de475a2</ns1:mRID>
            <!-- Business Type indicates if it is production, consumption or netted-->
            <ns1:businessType>A04</ns1:businessType>
            <!-- Indicates what was consumed/produced/netted -->
            <ns1:product>8716867000030</ns1:product>
            <!-- Unit of the measurements -->
            <ns1:energy_Measurement_Unit.name>WTT</ns1:energy_Measurement_Unit.name>
            <!-- The flow direction of the energy -->
            <ns1:flowDirection.direction>A02</ns1:flowDirection.direction>
            <ns1:Period>
                <!-- the resolution of the measurements in ISO 8601 duration -->
                <ns1:resolution>P0Y0M0DT0H15M0.000S</ns1:resolution>
                <ns1:timeInterval>
                    <!-- Start of the period -->
                    <ns1:start>2024-12-30T09:49Z</ns1:start>
                    <!-- End of the period -->
                    <ns1:end>2024-12-30T10:04Z</ns1:end>
                </ns1:timeInterval>
                <ns1:Point>
                    <!-- Is the index of the Point, the index starts with 1 and is always positive -->
                    <!-- The smallest value is always first, the points should be read in an ascending order -->
                    <ns1:position>1</ns1:position>
                    <!-- Measurement quantity -->
                    <ns1:energy_Quantity.quantity>10.0</ns1:energy_Quantity.quantity>
                    <!-- Describes if the value was measured, interpolated, etc -->
                    <ns1:energy_Quantity.quality>A04</ns1:energy_Quantity.quality>
                </ns1:Point>
                <!-- No errors specified, statically set, since it is required by the CIM -->
                <ns1:reason.code>999</ns1:reason.code>
            </ns1:Period>
            <!-- The mRID of the market evaluation point, for example the metering point ID -->
            <ns1:marketEvaluationPoint.mRID codingScheme="NFR">mid</ns1:marketEvaluationPoint.mRID>
            <!-- Indicates how the meter readings were aggregated if they were aggregated -->
            <ns1:marketEvaluationPoint.meterReadings.readings.readingType.aggregate>26</ns1:marketEvaluationPoint.meterReadings.readings.readingType.aggregate>
            <!-- Indicates what commodity was measured -->
            <ns1:marketEvaluationPoint.meterReadings.readings.readingType.commodity>0</ns1:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
            <!-- No errors specified, statically set, since it is required by the CIM -->
            <ns1:reason.code>999</ns1:reason.code>
        </ns1:TimeSeries>
    </ns1:MarketDocument>
</ns1:VHD_Envelope>
```

:::

The validated historical data market document contains the `Point` class, which represents a single measurement for a certain resolution.
To get the timestamp of a point, use the following equation:

$$TimeStepPosition = (startOfTimeInterval) + ((position-1) \times resolution)$$

> [!WARNING] Incorrect Position
> Some region connectors implement the position incorrectly, and return a timestamp instead of an index.
> We are currently working on fixing this issue.
> See https://github.com/eddie-energy/eddie/issues/2132
 