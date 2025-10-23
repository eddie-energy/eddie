# Implementing a Region Connector Frontend

The frontend of each region connector is to be implemented as a custom element.
The custom element will be loaded with the following attributes:

- `connection-id`: Optional value that can be used to identify a customer.
- `data-need-id`: Required by the backend to identify the requested data.
- `data-need-type`: Can be used to adjust content based on the type of data.
- `accounting-point-id`: Optional default for the accounting point ID.
- `jump-off-url`: Optional URL to link or redirect to after the permission request is submitted.
- `company-id`: Optional identifier of the permission administrator.

Elements should extend the [
`PermissionRequestFormBase`](https://github.com/eddie-energy/eddie/blob/main/region-connectors/shared/src/main/web/permission-request-form-base.js) class, which provides helpers for sending the permission request and sending user notifications.
The custom element should reside in `src/main/web` of the region connector.

The form base allows sending permission requests to the region connector.
This requires a form with the id `request-form` to be present and the custom element has to override the
`handleSubmit` method.
This method is responsible for creating the payload and sending the request to the region connector.

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

> [!Important]
> The `import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js"` line is required, otherwise the event handler is not attached to the `handleSubmit` method.

```javascript

import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    companyId: { attribute: "company-id" },
    companyName: { attribute: "company-name" },
    _requestStatus: { type: String },
  };

  constructor() {
    super();
  }

  connectedCallback() {
    super.connectedCallback();
    this.addEventListener("eddie-request-status", (event) => {
      const { status } = event.detail;
      this._requestStatus = status;
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const payload = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId
    };
    this.createPermissionRequest(payload)
      .catch((error) => this.error(error))
      .then((data) => {
        // Do something with the data
      });
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-button type="submit" variant="primary">Create</sl-button>
        </form>
      </div>
    `;
  }
}

export default PermissionRequestForm;
```