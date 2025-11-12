# CIM Mappings

This document should explain how [Region Connectors](../../../1-running/region-connectors/region-connectors.md) map incoming data from the metered data administrator or AIIDA instance to the corresponding CIM document.
This is especially relevant for Eligible Parties that already integrated a metered data administrator, but want to switch to the EDDIE Framework.

## AT EDA Mapping

This is the mapping for the [EDA Region Connector](../../../1-running/region-connectors/region-connector-at-eda.md).
The schemas used by EDA can be found at [ebutilities.at](https://www.ebutilities.at/schemas) (German only).

![Mapping for Validated Historical Data](./images/eda-vhd.svg "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/eda-ap.png "Accounting point data market document mapping")

## BE Fluvius Mapping

This is the mapping for the [Fluvius Region Connector](../../../1-running/region-connectors/region-connector-be-fluvius.md).
The schemas used by Fluvius have to be obtained from Fluvius itself.

![Mapping for Validated Historical Data](./images/fluvius-vhd.png "Validated historical data market document mapping")

## CDS Mapping

This is the mapping for the [CDS Region Connector](../../../1-running/region-connectors/region-connector-cds.md).
The schemas provided by the CDS project can be found at [cds.lfenergy.org](https://cds.lfenergy.org/).

![Mapping for Validated Historical Data](./images/cds-vhd.png "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/cds-ap.png "Accounting point data market document mapping")

## DK Energinet Mapping

This is the mapping for the [Energinet Region Connector](../../../1-running/region-connectors/region-connector-dk-energinet.md).
The schemas for the Energinet API can be found at [energinet.dk](https://energinet.dk/media/2l1lmb2z/customer-and-third-party-api-for-datahub-eloverblik-technical-description.pdf).

![Mapping for Validated Historical Data](./images/energinet-vhd.png "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/energinet-ap.png "Accounting point data market document mapping")

## ES Datadis Mapping

This is the mapping for the [Datadis Region Connector](../../../1-running/region-connectors/region-connector-es-datadis.md).
We are using an internal API for Datadis, so no official schema is available.

![Mapping for Validated Historical Data](./images/datadis-vhd.png "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/datadis-ap.png "Accounting point data market document mapping")

## FI Fingrid

This is the mapping for the [Fingrid Region Connector](../../../1-running/region-connectors/region-connector-fi-fingrid.md).
The schemas for the Fingrid API can be found at [support.datahub.fi](https://support.datahub.fi/fingrid).

![Mapping for Validated Historical Data](./images/fingrid-vhd.png "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/fingrid-ap.png "Accounting point data market document mapping")

## FR Enedis

This is the mapping for the [Enedis Region Connector](../../../1-running/region-connectors/region-connector-fr-enedis.md).
The schemas for the Enedis API can be found at [datahub-enedis.fr](https://datahub-enedis.fr/services-api/data-connect/documentation/).

![Mapping for Validated Historical Data](./images/enedis-vhd.png "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/enedis-ap.png "Accounting point data market document mapping")

## NL Mijn Aansluiting

This is the mapping for the [Mijn Aansluiting Region Connector](../../../1-running/region-connectors/region-connector-nl-mijn-aansluiting.md).
There are two APIs used by the Mijn Aansluiting Region Connector.
First, the [www.acc.mijnenergiedata.nl](https://www.acc.mijnenergiedata.nl/docs/) for validated historical data and second the [www.eancodeboek.nl](https://www.eancodeboek.nl/)  for accounting point data.

![Mapping for Validated Historical Data](images/mijn-aansluiting-nrtmd.png "Reference Energy Curve Near Real Time Measurement Market Document and Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/mijn-aansluiting-ap.png "Accounting point data market document mapping")

## US Green Button

This is the mapping for the [Green Button Region Connector](../../../1-running/region-connectors/region-connector-us-green-button.md).
The schemas for the UtilityAPI can be found at [utilityapi.com](https://utilityapi.com/docs).

![Mapping for Validated Historical Data](./images/green-button-vhd.png "Validated historical data market document mapping")

![Mapping for Accounting Point Data](./images/green-button-ap.png "Accounting point data market document mapping")

