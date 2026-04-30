# Energy Sharing Reference Data Market Documents

Energy Sharing Reference Data Market Documents to announce to the eligible party the participation factor of an accounting point in a specific Collective Energy Sharing Unit (CESU).
This document is emitted for permission requests with a [CESU Join Request Data Need](../../data-needs.md#cesujoinrequestdataneed).

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files can be found [here for v1.12](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/esr).
The following is an example of an energy sharing reference data market document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns:ESRDMD_Envelope xmlns:ns="https://eddie.energy/CIM/CEEDS_EnergySharingReferenceDataMarketDocument_v1.12_annotated.xsd">
    <ns:MarketDocument>
        <ns:mRID>AT000000000000000000000000000000000</ns:mRID>
        <ns:revisionNumber>112</ns:revisionNumber>
        <ns:createdDateTime>2025-10-06T07:04:58Z</ns:createdDateTime>
        <ns:sender_MarketParticipant.name>AT000000</ns:sender_MarketParticipant.name>
        <ns:receiver_MarketParticipant.name>CC000000</ns:receiver_MarketParticipant.name>
        <ns:EnergyCommunity>
            <ns:DateFrom>2025-10-05T22:00:00Z</ns:DateFrom>
            <ns:mRID>ATCC0000DYNAMCC000000000000000000</ns:mRID>
            <ns:AccountingPoint>
                <ns:energySharingParticipationFactor>100</ns:energySharingParticipationFactor>
                <ns:mRID codingScheme="NAT">AT0000000000000000000000000000000</ns:mRID>
                <ns:energySharingEnergyDirection>A02</ns:energySharingEnergyDirection>
            </ns:AccountingPoint>
        </ns:EnergyCommunity>
    </ns:MarketDocument>
    <ns:MessageDocumentHeader>
        <ns:creationDateTime>2026-04-30T07:42:42Z</ns:creationDateTime>
        <ns:MetaInformation>
            <ns:connectionId>cid</ns:connectionId>
            <ns:requestPermissionId>pid</ns:requestPermissionId>
            <ns:dataNeedId>dnid</ns:dataNeedId>
            <ns:documentType>energy-sharing-reference-data-market-document</ns:documentType>
            <ns:regionConnector>at-eda</ns:regionConnector>
            <ns:regionCountry>AT</ns:regionCountry>
        </ns:MetaInformation>
    </ns:MessageDocumentHeader>
</ns:ESRDMD_Envelope>
```
