# Implementing a Region Connector Frontend

The EDDIE button can include any custom element that is served on the `/ce.js` path.
While any setup can be used as long as it results in a custom element, existing region connector elements use the [Lit](https://lit.dev/) library, build with Vite, and extend a shared base class.
To replicate this setup simply copy the following files from an existing region connector. For example [/region-connectors/region-connector-at-eda](../../../region-connectors/region-connector-at-eda).

- `src/main/web/permission-request-form.js`
- `package.json`
- `vite.config.js`

You can then adjust the `permission-request-form.js` to match the requirements of your new region connector.

The custom element will be loaded with the following attributes:

- `core-url`: URL of the EDDIE Core for requesting additional application or data need information.
- `base-url`: URL of the region connector for making requests to the backend APIs.
- `connection-id`: Optional value that can be used to identify a customer.
- `data-need-id`: Required by the backend to identify the requested data.
- `data-need-type`: Can be used to adjust content based on the type of data.
- `country-code`: Country of the customer to adjust content if the region connector supports multiple countries.
- `accounting-point-id`: Optional default for the accounting point ID.
- `customer-identification`: Optional default for the customer identification.
- `jump-off-url`: Optional URL to link or redirect to after the permission request is submitted.
- `company-id`: Unique identifier of the permission administrator.
- `company-name`: The full legal name of the permission administrator.

Elements should only use existing components from the [Shoelace](https://shoelace.style/) library or shared custom elements.
There should be no need for custom CSS.
All used Shoelace elements need to be imported in the JS file to ensure they are loaded correctly.

Region connectors will typically include a form for the user to input the necessary data for the permission request.
The envisioned order of elements is:

1. Accounting point, customer identification (using the name used by the permission administrator)
2. Refresh tokens, API keys, address, or similar
3. Additional and optional fields
4. Submit button
5. Information on how to proceed after submit
6. Additional UI to load after submit

Which fields are present and required may vary between region connectors.

Fields and instructions should be provided in English and use the same terminology as the permission administrator.
Help texts on input fields are encouraged to guide the user in providing the correct information.

The [`PermissionRequestFormBase`](https://github.com/eddie-energy/eddie/blob/main/region-connectors/shared/src/main/web/permission-request-form-base.js) class provides helpers for sending the permission request, retrieving status updates, and sending user notifications.
To send permission requests through the base class a form with the id `request-form` has to be present and the custom element has to override the `handleSubmit` method.
This method is responsible for creating the payload and sending the request to the region connector.

To retrieve updates on the permission request status the element can subscribe to `eddie-request-status` events.
The event detail will include a `status` property matching the status.

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

    const formData = new FormData(event.target);

    const payload = {
      accountingPoint: formData.get("accountingPoint"),
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
    };
    this.createPermissionRequest(payload)
      .catch((error) => this.error(error))
      .then((data) => {
        // Do something with the data
      });
  }

  render() {
    return this._requestStatus !== "SENT_TO_PERMISSION_ADMINISTRATOR"
      ? html`
          <div>
            <form id="request-form">
              <sl-input
                label="Accounting Point"
                name="accountingPoint"
                type="text"
                required
              ></sl-input>
              <sl-button type="submit" variant="primary">Create</sl-button>
            </form>
          </div>
        `
      : html`
          <p>
            Your permission request was created.
            Please continue on the website of your permission administrator.
          </p>
          <sl-button href="${this.jumpOffUrl}>Continue</sl-button>
      `;
  }
}

export default PermissionRequestForm;
```
