# Acknowledgements

An acknowledgement is a message that confirms that a party received and processed a data message.
The eligible party can [enable acknowledgements in the data need](../1-running/data-need.md#acknowledgement) by setting the `acknowledgementRequired` field.

## Inbound Data

For an inbound permission, the EP sends data to AIIDA.
If the data need sets `acknowledgementRequired` to `true`, AIIDA sends an acknowledgement to the EP after AIIDA receives the data.
The acknowledgement is an [Acknowledgement Market Document](https://architecture.eddie.energy/framework/2-integrating/messages/cim/acknowledgement-market-documents.html#acknowledgement-market-document).
The EP reads the acknowledgement from the outbound connector of the EDDIE instance.
The market document MRID of the acknowledgement is identical to the MRID of the message that the EP sent.
The EP can use this MRID to correlate the acknowledgement with the transmitted data.

## Outbound Data

For an outbound permission, AIIDA sends data to the EP.
If the data need sets `acknowledgementRequired` to `true`, the EP is expected to send an acknowledgement to AIIDA after the EP receives the data.

## Related Documentation

- [Data Needs](../1-running/data-need.md#acknowledgement)
- [Inbound Data Source](../1-running/data-sources/mqtt/inbound/inbound-data-source.md)
- [Acknowledgement Market Document](https://architecture.eddie.energy/framework/2-integrating/messages/cim/acknowledgement-market-documents.html#acknowledgement-market-document)
