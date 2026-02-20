# Reference Energy Curve Min-Max Operation Envelope

The Reference Energy Curve Min-Max Operation Envelope is a document type defined in the Common Information Model (CIM) for the purpose of exchanging min-max envelopes for Flexible Connection Agreements (FCAs) **from the EP to the region connector**.
This document type is used to communicate the minimum and maximum power levels that a flexible connection agreement may operate at, which is crucial for grid management and optimization.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files for version v1.12 of this document can be found here: https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/recmmoe

The following is an example of a min-max envelope document for version v1.12:

```xml
<RECMMOE_Envelope xmlns="https//eddie.energy/CIM/RECMMOE_v1.12">
    <MessageDocumentHeader>
        <creationDateTime>2026-02-16T10:17:11Z</creationDateTime>
        <MetaInformation>
            <connectionId>1</connectionId>
            <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
            <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</dataNeedId>
            <documentType>min-max-envelope</documentType>
            <finalCustomerId>88e0fc2c-4ea7-4850-a736-8b9742757518</finalCustomerId>
            <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
            <defaultValues xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            <regionConnector>aiida</regionConnector>
            <regionCountry>AT</regionCountry>
            <Asset>
                <type>CONNECTION-AGREEMENT-POINT</type>
                <operatorId>AT003000</operatorId>
                <meterId>003114735</meterId>
            </Asset>
        </MetaInformation>
    </MessageDocumentHeader>
    <MarketDocument>
        <mRID>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</mRID>
        <description>Test Min-Max Envelope</description>
        <revisionNumber>1</revisionNumber>
        <lastModifiedDateTime>2026-02-16T10:17:11Z</lastModifiedDateTime>
        <comment>This is a test min-max envelope.</comment>
        <sender_MarketParticipant.mRID codingScheme="NAT">AT003000</sender_MarketParticipant.mRID>
        <sender_MarketParticipant.name>Netz Oberösterreich GmbH</sender_MarketParticipant.name>
        <sender_MarketParticipant.marketRole.type>CONNECTING_SYSTEM_OPERATOR</sender_MarketParticipant.marketRole.type>
        <receiver_MarketParticipant.mRID codingScheme="NAT">88e0fc2c-4ea7-4850-a736-8b9742757518
        </receiver_MarketParticipant.mRID>
        <receiver_MarketParticipant.name>Max Mustermann</receiver_MarketParticipant.name>
        <receiver_MarketParticipant.marketRole.type>FINAL_CUSTOMER</receiver_MarketParticipant.marketRole.type>
        <process.processType>MIN_MAX_ENVELOPE</process.processType>
        <period.timeInterval>
            <start>2026-06-01T00:00:00Z</start>
            <end>2026-06-30T23:59:59Z</end>
        </period.timeInterval>
        <TimeSeries_Series>
            <TimeSeries_Series>
                <mRID>series-1</mRID>
                <businessType>MIN_MAX_ENVELOPE</businessType>
                <curveType>MIN_MAX_ENVELOPE</curveType>
                <resourceTimeSeries.value1ScheduleType>loadReduction</resourceTimeSeries.value1ScheduleType>
                <flowDirection.direction>CONSUMPTION</flowDirection.direction>
                <registeredResource.mRID codingScheme="NAT">003114735</registeredResource.mRID>
                <registeredResource.name>Test Connection Point</registeredResource.name>
                <registeredResource.description>This is a test connection point for the min-max envelope.
                </registeredResource.description>
                <Series>
                    <Series>
                        <Period>
                            <Period>
                                <resolution xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                                <timeInterval>
                                    <start>2026-06-01T00:00:00Z</start>
                                    <end>2026-06-30T23:59:59Z</end>
                                </timeInterval>
                                <Point>
                                    <Point>
                                        <position>1</position>
                                        <min_Quantity.quantity>1</min_Quantity.quantity>
                                        <min_Quantity.quality>1</min_Quantity.quality>
                                        <max_Quantity.quantity>4</max_Quantity.quantity>
                                        <max_Quantity.quality>3</max_Quantity.quality>
                                    </Point>
                                </Point>
                            </Period>
                        </Period>
                    </Series>
                </Series>
            </TimeSeries_Series>
        </TimeSeries_Series>
    </MarketDocument>
</RECMMOE_Envelope>
```