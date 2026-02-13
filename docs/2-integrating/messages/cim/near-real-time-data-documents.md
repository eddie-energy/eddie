# Near Real-Time Data Documents

The Near Real-Time Data Document contains near-real time data that is received from a data source in AIIDA.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files for the different versions of this document can be found here:
- [v1.04](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_04/rtd)
- [v1.12](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/rtd)

The following is an example of a real-time data document for version v1.12:

```xml
<RTD_Envelope xmlns="https//eddie.energy/CIM/RTD_v1.12">
    <MessageDocumentHeader>
        <creationDateTime>2026-02-12T08:03:40Z</creationDateTime>
        <MetaInformation>
            <connectionId>1</connectionId>
            <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
            <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</dataNeedId>
            <documentType>near-real-time-market-document</documentType>
            <finalCustomerId>88e0fc2c-4ea7-4850-a736-8b9742757518</finalCustomerId>
            <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
            <defaultValues xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            <regionConnector>aiida</regionConnector>
            <regionCountry>AT</regionCountry>
            <Asset>
                <type>CONNECTION-AGREEMENT-POINT</type>
                <operatorId xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                <meterId xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            </Asset>
        </MetaInformation>
    </MessageDocumentHeader>
    <MarketDocument>
        <mRID>78f93c55-c666-43b3-bbf2-a07059cad002</mRID>
        <createdDateTime>2026-02-12T08:03:40Z</createdDateTime>
        <TimeSeries>
            <TimeSeries>
                <version>1.0</version>
                <dateAndOrTime.dateTime>2026-02-12T08:03:38Z</dateAndOrTime.dateTime>
                <Quantity>
                    <Quantity>
                        <quantity>0.117</quantity>
                        <type>2</type>
                        <quality>AS_PROVIDED</quality>
                    </Quantity>
                    <Quantity>
                        <quantity>65238.377</quantity>
                        <type>0</type>
                        <quality>AS_PROVIDED</quality>
                    </Quantity>
                </Quantity>
                <registeredResource.mRID codingScheme="NAT">
                    0743c9d8-3e5f-4575-999b-34f6f83b2075
                </registeredResource.mRID>
            </TimeSeries>
        </TimeSeries>
    </MarketDocument>
</RTD_Envelope>
```