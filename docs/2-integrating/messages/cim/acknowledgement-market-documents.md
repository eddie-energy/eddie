# Acknowledgement Market Document

The Acknowledgement Market Document (ACK) is a document used to acknowledge the receipt of a market document.
It contains information about the received document, such as its mRID, revision number, type, and the reason for rejection if applicable.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files for version v1.12 of this document can be found here: https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/ack

The following is an example of a acknowledgement envelope for version v1.12:

```xml

<Acknowledgement_Envelope xmlns="https://eddie.energy/CIM/ACK_v1.12">
    <MessageDocumentHeader>
        <creationDateTime>2026-02-24T11:58:05Z</creationDateTime>
        <MetaInformation>
            <connectionId>1</connectionId>
            <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
            <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</dataNeedId>
            <documentType>acknowledgement-market-document</documentType>
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
        <mRID>1cb72828-e869-463c-a447-7062bcca24b4</mRID>
        <createdDateTime>2026-02-24T11:58:05Z</createdDateTime>
        <sender_MarketParticipant.mRID codingScheme="NAT">AT003000</sender_MarketParticipant.mRID>
        <sender_MarketParticipant.marketRole.type>CONNECTING_SYSTEM_OPERATOR</sender_MarketParticipant.marketRole.type>
        <receiver_MarketParticipant.mRID codingScheme="NAT">88e0fc2c-4ea7-4850-a736-8b9742757518
        </receiver_MarketParticipant.mRID>
        <receiver_MarketParticipant.marketRole.type>FINAL_CUSTOMER</receiver_MarketParticipant.marketRole.type>
        <received_MarketDocument.mRID>5a1cd7e9-345a-4c5a-94e3-987b3b8d2def</received_MarketDocument.mRID>
        <received_MarketDocument.revisionNumber>1</received_MarketDocument.revisionNumber>
        <received_MarketDocument.type>min-max-envelope</received_MarketDocument.type>
        <received_MarketDocument.process.processType>MIN_MAX_ENVELOPE</received_MarketDocument.process.processType>
        <received_MarketDocument.title xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        <received_MarketDocument.createdDateTime>2026-02-24T11:57:05Z</received_MarketDocument.createdDateTime>
        <Rejected_TimeSeries>
            <Rejected_TimeSeries>
                <mRID>63c99336-d854-4ed2-be01-13dca22a2850</mRID>
                <version>1</version>
                <InError_Period>
                    <InError_Period>
                        <timeInterval>
                            <start>2026-02-24T12:58:05Z</start>
                            <end>2026-02-24T13:58:05Z</end>
                        </timeInterval>
                    </InError_Period>
                </InError_Period>
                <Reason>
                    <Reason>
                        <code>A01</code>
                        <text>Invalid time series data</text>
                    </Reason>
                </Reason>
            </Rejected_TimeSeries>
        </Rejected_TimeSeries>
    </MarketDocument>
</Acknowledgement_Envelope>
```