# Near Real-Time Data Market Documents

The Near Real-Time Data Market Document contains near-real time data that is received from a data source in AIIDA.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files can be found [here](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_04/rtd).
The following is an example of a validated historical data market document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:RTD_Envelope
        xmlns="http://www.eddie.energy/VHD/EDD01/20240614"
        xmlns:ns2="https://eddie.energy/CIM/RTD_v1.04"
        xmlns:ns3="http://www.eddie.energy/Consent/EDD02/20240125"
        xmlns:ns4="htthttp://www.eddie.energy/AP/EDD04/20240422"
        xmlns:ns5="https//eddie.energy/CIM/VHD_v1.04"
        xmlns:ns6="https://eddie.energy/CIM/RTR">
    <ns2:messageDocumentHeader.creationDateTime>2025-11-11T15:18:20Z</ns2:messageDocumentHeader.creationDateTime>
    <ns2:messageDocumentHeader.metaInformation.connectionId>1</ns2:messageDocumentHeader.metaInformation.connectionId>
    <ns2:messageDocumentHeader.metaInformation.dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</ns2:messageDocumentHeader.metaInformation.dataNeedId>
    <ns2:messageDocumentHeader.metaInformation.documentType>near-real-time-market-document</ns2:messageDocumentHeader.metaInformation.documentType>
    <ns2:messageDocumentHeader.metaInformation.permissionId>751c1998-491a-4d18-900b-bda5ab8934f3</ns2:messageDocumentHeader.metaInformation.permissionId>
    <ns2:messageDocumentHeader.metaInformation.finalCustomerId>f52db327-1d59-4914-9a7d-13f4624ecb67</ns2:messageDocumentHeader.metaInformation.finalCustomerId>
    <ns2:messageDocumentHeader.metaInformation.asset>CONNECTION-AGREEMENT-POINT</ns2:messageDocumentHeader.metaInformation.asset>
    <ns2:messageDocumentHeader.metaInformation.dataSourceId>59053d98-0431-456d-a496-d9571c8da827</ns2:messageDocumentHeader.metaInformation.dataSourceId>
    <ns2:messageDocumentHeader.metaInformation.region.connector>aiida</ns2:messageDocumentHeader.metaInformation.region.connector>
    <ns2:messageDocumentHeader.metaInformation.region.country>AT</ns2:messageDocumentHeader.metaInformation.region.country>
    <ns2:MarketDocument>
        <ns2:mRID>a9532437-2054-4443-a7d3-4fdf858d66d3</ns2:mRID>
        <ns2:createdDateTime>2025-11-11T15:18:20Z</ns2:createdDateTime>
        <ns2:TimeSeries>
            <ns2:version>1.0</ns2:version>
            <ns2:registeredResource.mRID codingScheme="NAT">59053d98-0431-456d-a496-d9571c8da827</ns2:registeredResource.mRID>
            <ns2:dateAndOrTime.dateTime>2025-11-11T15:18:17Z</ns2:dateAndOrTime.dateTime>
            <ns2:Quantity>
                <ns2:quantity>1951</ns2:quantity>
                <ns2:type>0</ns2:type>
                <ns2:quality>AS_PROVIDED</ns2:quality>
            </ns2:Quantity>
            <ns2:Quantity>
                <ns2:quantity>1693</ns2:quantity>
                <ns2:type>2</ns2:type>
                <ns2:quality>AS_PROVIDED</ns2:quality>
            </ns2:Quantity>
        </ns2:TimeSeries>
    </ns2:MarketDocument>
</ns2:RTD_Envelope>
```