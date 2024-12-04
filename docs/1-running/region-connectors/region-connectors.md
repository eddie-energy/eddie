# Region-Connectors

A region-connector connects EDDIE with a permission administrator (PA) and metered data administrators (MDA) of a region.
There can be multiple region-connectors for a region, since there can be multiple PAs and MDAs per region.

A PAs responsibility is to administer permissions to data of final customers.
When a third party, here called the eligible party (EP), requests access to the data of a final customer, they have to do that via the PA.
The PA validates the permission request for the data and sends it to the final customer.
The final customer can deny or accept the permission request.
A final customer is an entity with one or more metering points.
A metering point can provide data on multiple utilities such as electricity or gas.

The MDA is responsible for providing access to the data that the EP provided.
They verify with the PA if the final customer consented to share the data with the EP.