# Messages and Documents

EDDIE emits several messages and documents that can be used by the EP to react to permission request status changes, as well as collect the data that was requested from final customers.
There are two types of messages:

- CIM documents: Those are documents that respect the CIM schema, which can be found [here](https://github.com/eddie-energy/eddie/tree/main/cim/src/main/schemas/cim).
- EDDIE's internal format, which is not standardized, but can provide more detailed data or concise messages.

> [!IMPORTANT]
> These messages and documents are only exchanged between the eligible party and EDDIE, neither the permission administrator nor the metered data administrator are directly involved in the creation of the messages and documents.

## Message flow

EDDIE emits a number of different messages at different points during its runtime.
The following describes which messages are sent at what point during the interaction of a final customer with EDDIE and afterward.

1. The final customer opens the EDDIE Button, selects a country, and a permission administrator.
The final customer creates a [permission request](../integrating.md#permission-requests) by clicking on the create-button in the EDDIE Button.
This creates the permission request on EDDIE's side and a [connection status message (CSM)](agnostic.md#connection-status-messages) and a [permission market document (PMD)](./cim/permission-market-documents.md) is sent to the active outbound connectors indicating the creation of the permission request.
For all statuses, a permission request can have see [the permission process model](../integrating.md#permission-process-model)
2. EDDIE validates the permission request.
Then another set of CSM and PMD are emitted indicating the validity of the permission request, either malformed or validated.
3. Then the permission request is sent to the permission administrator, and another set of CSM and PMD are emitted, which indicate whether the permission request could be sent to the PA or not.
4. The PA then notifies EDDIE whether the permission request is invalid, the final customer ignoring it or if it was accepted or rejected by the final customer.
This leads to the emittion of CSM and PMD indicating the results described above.
5. Upon acceptance, EDDIE starts receiving or polling data from the MDA. 
The data is emitted via [the validated historical data market document](./cim/validated-historical-data-market-documents.md) or [the accounting point market document](./cim/accounting-point-data-market-documents.md) depending on [the data need](../data-needs.md) that was used to create the permission request.
Furthermore, the data is forwarded **as received from the MDA** via [raw data messages](./agnostic.md#raw-data-messages).
At this point, it is possible for the eligible party to request the termination of the permission request.
This is done via [the termination document](./cim/permission-market-documents.md#termination-documents).
If the data that is received is incomplete, it is possible to re-request data via [the redistribution transaction request document](./cim/redistribution-transaction-request-documents.md).
6. If the final customer revokes the permission, EDDIE emits a CSM and PMD notifying the eligible party.
If the eligible party terminates a permission request, they are notified about the status of the permission request in the termination process described in [the permission process model](../integrating.md#permission-process-model).
Same, if the permission request is unfulfillable or fulfilled, it is automatically terminated at the PA's side if necessary.