# Validated Historical Data Market Documents

Validated historical data market documents contain metered data that is received from the MDA.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files can be found [here](https://github.com/eddie-energy/eddie/tree/main/api/src/main/schemas/cim/xsd/v0_82/vhd).
The following is an example of a validated historical data market document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns3:ValidatedHistoricalData_Envelope
        xmlns="http://www.eddie.energy/Consent/EDD02/20240125"
        xmlns:ns2="htthttp://www.eddie.energy/AP/EDD04/20240422"
        xmlns:ns3="http://www.eddie.energy/VHD/EDD01/20240614">
    <ns3:MessageDocumentHeader>
        <!-- Omitted for conciseness -->
    </ns3:MessageDocumentHeader>
    <ns3:ValidatedHistoricalData_MarketDocument>
        <!-- Random UUID identifying this document -->
        <ns3:mRID>183bfc39-22fe-4bc2-bf31-7436b075c6e0</ns3:mRID>
        <!-- The CIM version of the document -->
        <ns3:revisionNumber>0.82</ns3:revisionNumber>
        <!-- Identifies it as a measurement document -->
        <ns3:type>A45</ns3:type>
        <!-- created datetime of this document -->
        <ns3:createdDateTime>2024-12-02T10:28:06Z</ns3:createdDateTime>
        <!-- Sender of the data -->
        <ns3:sender_MarketParticipant.mRID>
            <ns3:codingScheme>A02</ns3:codingScheme>
            <ns3:value>DEMOUTILITY</ns3:value>
        </ns3:sender_MarketParticipant.mRID>
        <!-- Role of the sender, mostly metering point administrators -->
        <ns3:sender_MarketParticipant.marketRole.type>A26</ns3:sender_MarketParticipant.marketRole.type>
        <!-- Receiver of the data -->
        <ns3:receiver_MarketParticipant.mRID>
            <ns3:codingScheme>NAT</ns3:codingScheme>
            <!-- The identifier of the eligible party at the MDA, often client ID or partner ID -->
            <ns3:value>CLIENT_ID</ns3:value>
        </ns3:receiver_MarketParticipant.mRID>
        <!-- Role of the receiver -->
        <ns3:receiver_MarketParticipant.marketRole.type>A13</ns3:receiver_MarketParticipant.marketRole.type>
        <ns3:process.processType>A16</ns3:process.processType>
        <ns3:period.timeInterval>
            <!-- The start of the first measurement -->
            <ns3:start>2024-12-01T00:00Z</ns3:start>
            <!-- The end of the last measurement -->
            <ns3:end>2024-12-02T00:00Z</ns3:end>
        </ns3:period.timeInterval>
        <!-- contains the measurement data of one meter -->
        <ns3:TimeSeriesList>
            <ns3:TimeSeries>
                <!-- Random UUID of this timeseries -->
                <ns3:mRID>4105d729-d044-402d-b76a-2c6076e73bd5</ns3:mRID>
                <!-- Business Type indicates if it is production, consumption or netted-->
                <ns3:businessType>A01</ns3:businessType>
                <!-- Indicates what was consumed/produced/netted -->
                <ns3:product>8716867000016</ns3:product>
                <!-- The flow direction of the energy -->
                <ns3:flowDirection.direction>A01</ns3:flowDirection.direction>
                <!-- Metering Point ID -->
                <ns3:marketEvaluationPoint.mRID>
                    <ns3:codingScheme>A02</ns3:codingScheme>
                    <ns3:value>1788743</ns3:value>
                </ns3:marketEvaluationPoint.mRID>
                <!-- Describes how the values of a measuring period were combined, for example, average over 15 minutes -->
                <ns3:marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation>26
                </ns3:marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation>
                <!-- How the data was obtained, for example, primary metered and secondary metered -->
                <ns3:marketEvaluationPoint.meterReadings.readings.ReadingType.commodity>1
                </ns3:marketEvaluationPoint.meterReadings.readings.ReadingType.commodity>
                <!-- Unit of the measurements -->
                <ns3:energy_Measurement_Unit.name>KWH</ns3:energy_Measurement_Unit.name>
                <ns3:Series_PeriodList>
                    <ns3:Series_Period>
                        <ns3:timeInterval>
                            <!-- Start of the series period -->
                            <ns3:start>2024-12-01T00:00Z</ns3:start>
                            <!-- End of the series period -->
                            <ns3:end>2024-12-01T00:15Z</ns3:end>
                        </ns3:timeInterval>
                        <!-- The resolution of the point list following the ISO 8601 duration -->
                        <!-- For the resolutions supported by EDDIE see: https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/Granularity.html -->
                        <ns3:resolution>PT15M</ns3:resolution>
                        <ns3:PointList>
                            <ns3:Point>
                                <!-- Can contain a timestamp of the start of the measurement of the point or a simple index -->
                                <!-- The smallest value is always first, the points should be read in an ascending order -->
                                <ns3:position>1733011200</ns3:position>
                                <!-- Measurement quantity -->
                                <ns3:energy_Quantity.quantity>80000</ns3:energy_Quantity.quantity>
                                <!-- Describes if the value was measured, interpolated, etc -->
                                <ns3:energy_Quantity.quality>A04</ns3:energy_Quantity.quality>
                            </ns3:Point>
                        </ns3:PointList>
                    </ns3:Series_Period>
                </ns3:Series_PeriodList>
                <ns3:ReasonList>
                    <ns3:Reason>
                        <!-- No errors specified, statically set, since it is required by the CIM -->
                        <ns3:code>999</ns3:code>
                    </ns3:Reason>
                </ns3:ReasonList>
            </ns3:TimeSeries>
        </ns3:TimeSeriesList>
    </ns3:ValidatedHistoricalData_MarketDocument>
</ns3:ValidatedHistoricalData_Envelope>
```
