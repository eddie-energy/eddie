---
prev:
  text: "Accounting Point Data Market Documents"
  link: "./accounting-point-data-market-documents.md"
next:
  text: "Data Needs"
  link: "../data-needs.md"
---

# Redistribution Transaction Request Documents (RTR Documents)

Redistribution Transaction Request Documents (RTR Documents) are a CIM document that allows the eligible party to request validated historical data for a specific timeframe for a specific permission request.
The timeframe must lie in the timeframe of the permission request.
Furthermore, this only works for permission requests that are either accepted or fulfilled, if they have been externally terminated it will not be possible to request any validated historical data using that permission request.
The main purpose of the RTR documents is to request validated historical data that was left out be the metered data administrator the first time the data was requested.
This can happen for various reasons, such as accidentally not the whole timeframe that is requested by the EDDIE framework for a permission request.
The purpose of the RTR documents is **not** that the eligible party can request the data again so they do not have to save it themselves.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.
> The following shows an example of the RTR document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns4:RTR_Envelope
        xmlns:ns4="https://eddie.energy/CIM">
    <!-- When the eligible party created this document -->
    <ns4:messageDocumentHeader.creationDateTime>2025-01-03T00:00:00Z</ns4:messageDocumentHeader.creationDateTime>
    <!-- The permission request for the data that should be requested -->
    <ns4:messageDocumentHeader.metaInformation.permissionId>
        permissionId
    </ns4:messageDocumentHeader.metaInformation.permissionId>
    <!-- The region connector that owns the permission request -->
    <ns4:messageDocumentHeader.metaInformation.region.connector>
        rc-id
    </ns4:messageDocumentHeader.metaInformation.region.connector>
    <!-- The timeframe of the to-be requested data -->
    <ns4:marketDocument.period.timeInterval>
        <ns4:start>2025-01-01T01:01Z</ns4:start>
        <ns4:end>2025-01-02T00:00Z</ns4:end>
    </ns4:marketDocument.period.timeInterval>
</ns4:RTR_Envelope>
```
