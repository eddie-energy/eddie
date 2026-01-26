# Accounting Point Data Market Documents

Accounting point data market documents provide information to one or multiple metering points related to one permission request.
They can contain information such as contract details, address of the metering point, and type of meter installed.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files can be found [here](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v0_82/ap).
The following is an example of an accounting point market document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:AccountingPoint_Envelope xmlns="http://www.eddie.energy/Consent/EDD02/20240125"
                              xmlns:ns2="htthttp://www.eddie.energy/AP/EDD04/20240422"
                              xmlns:ns3="http://www.eddie.energy/VHD/EDD01/20240614">
    <ns2:MessageDocumentHeader>
        <!-- Omitted for conciseness -->
    </ns2:MessageDocumentHeader>
    <ns2:AccountingPoint_MarketDocument>
        <!-- UUID identifying this document -->
        <ns2:mRID>e3c02c31-9524-4d6e-8d31-c3886a820472</ns2:mRID>
        <!-- Version of the CIM -->
        <ns2:revisionNumber>0.82</ns2:revisionNumber>
        <!-- Identifies the type of document, always Accounting Point Master Data -->
        <ns2:type>B99</ns2:type>
        <!-- created datetime of this document -->
        <ns2:createdDateTime>2024-12-03T10:27:40Z</ns2:createdDateTime>
        <!-- Identifies the sender -->
        <ns2:sender_MarketParticipant.mRID>
            <ns2:codingScheme>A10</ns2:codingScheme>
            <ns2:value>0000000000000</ns2:value>
        </ns2:sender_MarketParticipant.mRID>
        <!-- The role of the sender, in this case metering point administrator -->
        <ns2:sender_MarketParticipant.marketRole.type>A26</ns2:sender_MarketParticipant.marketRole.type>
        <!-- The receiver of the document, in this case, EDDIE -->
        <ns2:receiver_MarketParticipant.mRID>
            <ns2:codingScheme>NAT</ns2:codingScheme>
            <ns2:value>REPLACE_ME</ns2:value>
        </ns2:receiver_MarketParticipant.mRID>
        <!-- Role of the receiver, here consumer -->
        <ns2:receiver_MarketParticipant.marketRole.type>A13</ns2:receiver_MarketParticipant.marketRole.type>
        <!-- A list of accounting points, aka metering points -->
        <ns2:AccountingPointList>
            <ns2:AccountingPoint>
                <!-- Specifies how the energy volumes are treated for settlement for the AccountingPoint -->
                <ns2:settlementMethod>D01</ns2:settlementMethod>
                <ns2:mRID>
                    <!-- Country of the metering point -->
                    <ns2:codingScheme>NDK</ns2:codingScheme>
                    <!-- The metering point ID -->
                    <ns2:value>000000000000000000</ns2:value>
                </ns2:mRID>
                <!-- The length of time between meter readings -->
                <ns2:meterReadingResolution>PT1H</ns2:meterReadingResolution>
                <!-- The definition of the number of units of time that compose an individual step within a period -->
                <ns2:resolution>PT1H</ns2:resolution>
                <!-- The kind of commodity being measured -->
                <ns2:commodity>2</ns2:commodity>
                <!-- The direction of that the meter reads, UP - Production, DOWN - Consumption -->
                <ns2:direction>A02</ns2:direction>
                <!-- The supply status either 'on' or 'off' -->
                <ns2:supplyStatus>E22</ns2:supplyStatus>
                <!-- List of contract parties -->
                <ns2:ContractPartyList>
                    <ns2:ContractParty>
                        <!-- Describes the type of role the party has -->
                        <ns2:contractPartyRole>contractPartner</ns2:contractPartyRole>
                        <ns2:surName>John Doe</ns2:surName>
                        <ns2:email>john@doe.com</ns2:email>
                    </ns2:ContractParty>
                </ns2:ContractPartyList>
                <ns2:AddressList>
                    <ns2:Address>
                        <!-- The address where the meter is installed -->
                        <ns2:addressRole>delivery</ns2:addressRole>
                        <ns2:postalCode>1010</ns2:postalCode>
                        <ns2:cityName>some city</ns2:cityName>
                        <ns2:streetName>some street</ns2:streetName>
                        <ns2:buildingNumber>1</ns2:buildingNumber>
                        <ns2:floorNumber>1</ns2:floorNumber>
                        <ns2:doorNumber/>
                    </ns2:Address>
                    <ns2:Address>
                        <!-- The invoice address -->
                        <ns2:addressRole>invoice</ns2:addressRole>
                        <ns2:postalCode>1010</ns2:postalCode>
                        <ns2:cityName>some city</ns2:cityName>
                        <ns2:streetName>some street</ns2:streetName>
                        <ns2:buildingNumber>1</ns2:buildingNumber>
                        <ns2:floorNumber>1</ns2:floorNumber>
                        <ns2:doorNumber/>
                    </ns2:Address>
                </ns2:AddressList>
            </ns2:AccountingPoint>
        </ns2:AccountingPointList>
    </ns2:AccountingPoint_MarketDocument>
</ns2:AccountingPoint_Envelope>
```

