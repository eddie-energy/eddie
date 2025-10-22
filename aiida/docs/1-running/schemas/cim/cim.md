# Common Information Model (CIM)

The CIM provides its schema as XSD files which AIIDA needs to respect when emitting the documents.

AIIDA supports exactly one CIM document:

- [Real Time Data Market Document](real-time-data-market-document.md)

AIIDA is able to convert near-real time (NRT) data into the Real Time Data Document.
AIIDA has also the capability to receive data in form of the Time Series provided by the Real Time Data Document with
the use of the CIM Datasource Adapter.

## CIM envelope for v1.04

```xml

<RTD_Envelope>
    <!-- The datetime when the envelope of the document was created  -->
    <messageDocumentHeader.creationDateTime>2025-07-01T09:44:00.00040249Z</messageDocumentHeader.creationDateTime>
    <!-- The connection ID is given by the eligible party for one or more permission requests -->
    <messageDocumentHeader.metaInformation.connectionId>3</messageDocumentHeader.metaInformation.connectionId>
    <!-- The Data Need ID shows to which Data Need a permission request is related to, and in turn which data is related to it too -->
    <messageDocumentHeader.metaInformation.dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84
    </messageDocumentHeader.metaInformation.dataNeedId>
    <!-- The document type -->
    <messageDocumentHeader.metaInformation.documentType>near-real-time-market-document
    </messageDocumentHeader.metaInformation.documentType>
    <!-- The permission ID uniquely identifies a permission request in AIIDA and EDDIE -->
    <messageDocumentHeader.metaInformation.permissionId>150cfd97-64bb-402b-838f-57f8605713b7
    </messageDocumentHeader.metaInformation.permissionId>
    <!-- The UUID of the AIIDA application -->
    <messageDocumentHeader.metaInformation.finalCustomerId>008cf1d6-e118-45a8-bc17-a331dfc57e77
    </messageDocumentHeader.metaInformation.finalCustomerId>
    <!-- The asset type of the data source  -->
    <messageDocumentHeader.metaInformation.asset>CONNECTION-AGREEMENT-POINT
    </messageDocumentHeader.metaInformation.asset>
    <!-- The UUID of the data source which provided the data -->
    <messageDocumentHeader.metaInformation.dataSourceId>5eef407d-d14f-49d4-b61a-769a20caa540
    </messageDocumentHeader.metaInformation.dataSourceId>
    <!-- The Region Connector used to create the permission -->
    <messageDocumentHeader.metaInformation.regionConnector>aiida</messageDocumentHeader.metaInformation.regionConnector>
    <!-- The country the data is from -->
    <messageDocumentHeader.metaInformation.regionCountry>AT</messageDocumentHeader.metaInformation.regionCountry>
    <marketDocument>
        <!-- The Real Time Data Market Document -->
    </marketDocument>
</RTD_Envelope>
```