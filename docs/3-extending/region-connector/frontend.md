---
prev:
  text: "Configuration"
  link: "./configuration.md"
next:
  text: "Beans of Interest"
  link: "./beans-of-interest.md"
---

# Implementing a Region Connector Frontend

The frontend of each region connector is to be implemented as a custom element.
The custom element will be loaded with the following attributes:

- `connection-id`: Optional value that can be used to identify a customer.
- `data-need-id`: Required by the backend to identify the requested data.
- `accounting-point-id`: Optional default for the accounting point ID.
- `jump-off-url`: Optional URL to link or redirect to after the permission request is submitted.
- `company-id`: Optional identifier of the permission administrator.

Elements should extend the
`PermissionRequestFormBase` class, which provides helpers for sending the permission request and sending user notifications.
The custom element should reside in

Elements should only use existing components from the [Shoelace](https://shoelace.style/) library or shared custom elements.
There should be no need for custom CSS.

Region connectors will typically include a form for the user to input the necessary data for the permission request.
The envisioned order of elements is:

1. Accounting Point ID (using the name used by the permission administrator)
2. Refresh tokens, API keys, address, or similar
3. Additional and optional fields
4. Submit button
5. Information on how to proceed after submit
6. Additional UI to load after submit

Which fields are present and required may vary between region connectors.

Fields and instructions should be provided in English and use the same terminology as the permission administrator.
Help texts on input fields are encouraged to guide the user in providing the correct information.
