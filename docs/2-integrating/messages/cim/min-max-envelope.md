# Reference Energy Curve Min-Max Operation Envelope

The Reference Energy Curve Min-Max Operation Envelope is a document type defined in the Common Information Model (CIM) for the purpose of exchanging min-max envelopes for Flexible Connection Agreements (FCAs) **from the EP to the region connector**.
This document type is used to communicate the minimum and maximum power levels that a flexible connection agreement may operate at, which is crucial for grid management and optimization.

> [!Warning]
> Only the XSD is provided since the CIM is an XML first format, and it cannot be guaranteed that the JSON variant is stable.

The XSD files for version v1.12 of this document can be found here: https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim/xsd/v1_12/recmmoe

The following is an example of a min-max envelope document for version v1.12:

<<< ./xml/min-max-envelope-example.xml#snippet