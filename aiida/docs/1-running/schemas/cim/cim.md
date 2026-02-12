# Common Information Model (CIM)

The CIM provides its schema as XSD files which AIIDA needs to respect when emitting the documents.

AIIDA supports exactly one CIM document:

- [Real Time Data Market Document](real-time-data-market-document.md) in versions:
  - v1.04
  - v1.12

AIIDA is able to convert near-real time (NRT) data into the Real Time Data Document.
AIIDA has also the capability to receive data in form of the Time Series provided by the Real Time Data Document with
the use of the CIM Datasource Adapter.

## CIM envelope for v1.12

```xml
<RTD_Envelope xmlns="https//eddie.energy/CIM/RTD_v1.12">
  <MessageDocumentHeader>
    <!-- The datetime when the envelope of the document was created  -->
    <creationDateTime>2026-02-12T08:03:40Z</creationDateTime>
    <MetaInformation>
      <!-- The connection ID is given by the eligible party for one or more permission requests -->
      <connectionId>1</connectionId>
      <!-- The permission ID uniquely identifies a permission request in AIIDA and EDDIE -->
      <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
      <!-- The Data Need ID shows to which Data Need a permission request is related to, and in turn which data is related to it too -->
      <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</dataNeedId>
      <!-- The document type -->
      <documentType>near-real-time-market-document</documentType>
      <!-- The UUID of the AIIDA application -->
      <finalCustomerId>88e0fc2c-4ea7-4850-a736-8b9742757518</finalCustomerId>
      <!-- The UUID of the data source which provided the data -->
      <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
      <!-- The Region Connector used to create the permission -->
      <regionConnector>aiida</regionConnector>
      <!-- The country the data is from -->
      <regionCountry>AT</regionCountry>
      <Asset>
        <!-- The asset type of the data source  -->
        <type>CONNECTION-AGREEMENT-POINT</type>
        <!-- ID of the operator of the asset -->
        <operatorId>AT003000</operatorId>
        <!-- The ID of the asset, e.g. the meter ID -->
        <meterId>003114735</meterId>
      </Asset>
    </MetaInformation>
  </MessageDocumentHeader>
  <MarketDocument>
    <!-- The Real Time Data Market Document -->
  </MarketDocument>
</RTD_Envelope>
```