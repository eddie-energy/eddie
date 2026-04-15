# Energy Sharing Reference Data Market Documents

Energy Sharing Reference Data Market Documents to announce to the eligible party the participation factor of an accounting point in a specific Collective Energy Sharing Unit (CESU).
This document is emitted for permission requests with a [CESU Join Request Data Need](../../data-needs.md#cesujoinrequestdataneed).

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files can be found [here for v1.12](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/esr).
The following is an example of an energy sharing reference data market document.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns:Envelope xmlns:ns="https://eddie.energy/CIM/CEEDS_EnergySharingReferenceDataMarketDocument_annotated_v1.12_new.xsd">
    <ns:MessageDocumentHeader>
        <!-- Omitted for conciseness -->
    </ns:MessageDocumentHeader>
    <ns:MarketDocument>
        <!-- Full example will be made available once the Austrian region connector implements the mapping in order to create a realistic example. See GH-2439 -->
    </ns:MarketDocument>
</ns:Envelope>
```
