import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    coreUrl: { attribute: "core-url" },
    dataNeedType: { attribute: "data-need-type" },
    _isPermissionRequestCreated: { type: Boolean },
  };

  handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);

    const payload = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
      verificationCode: formData.get("verificationCode"),
      postalCode: formData.get("postalCode"),
    };

    this.createPermissionRequest(payload, {
      credentials: "include",
    })
      .then(({ redirectUri }) => {
        window.open(redirectUri, "_blank");
      })
      .catch((error) => this.error(error));
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-input
            label="House Number"
            type="text"
            helpText="This is the house number of your address. It is required by Mijn Aansluiting to verify that this is the correct metering point."
            name="verificationCode"
            placeholder="House Number"
            required
          ></sl-input>
          ${this.dataNeedType.includes("account")
            ? html`<sl-input
                label="Postal Code"
                type="text"
                pattern="\\d{4}[A-Z]{2}"
                helpText="This is the postal code of your address."
                name="postalCode"
                placeholder="Postal Code"
                required
              ></sl-input>`
            : ``}
          <p>
            By clicking on this button, you will access your personal Mijn
            Aansluiting account where you can authorise Mijn Aansluiting to send
            us your data.
          </p>

          <sl-button type="submit" variant="primary">Submit</sl-button>
        </form>
      </div>
    `;
  }
}

export default PermissionRequestForm;
