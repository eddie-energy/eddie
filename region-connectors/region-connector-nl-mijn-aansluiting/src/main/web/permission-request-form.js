import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";

const BASE_URL = new URL(import.meta.url).href
  .replace("ce.js", "")
  .slice(0, -1);
const REQUEST_URL = BASE_URL + "/permission-request";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    _requestId: { type: String },
    _isPermissionRequestCreated: { type: Boolean },
  };

  constructor() {
    super();

    this._isPermissionRequestCreated = false;
  }

  handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);

    const jsonData = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedAttributes.id,
      verificationCode: formData.get("verificationCode"),
    };

    this.createPermissionRequest(jsonData)
      .then()
      .catch((error) => this.error(error));
  }

  async createPermissionRequest(payload) {
    const response = await fetch(REQUEST_URL, {
      body: JSON.stringify(payload),
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
    });

    const result = await response.json();

    if (response.status === 201) {
      const location = response.headers.get("Location");

      if (!location) {
        throw new Error("Header 'Location' is missing");
      }

      this._isPermissionRequestCreated = true;

      this.handlePermissionRequestCreated(BASE_URL + location);
      window.open(result["redirectUri"], "_blank");
    } else if (response.status === 400) {
      // An error on the client side happened, and it should be displayed as alert in the form
      let errorMessage;

      if (result["errors"] == null || result["errors"].length === 0) {
        errorMessage =
          "Something went wrong when creating the permission request, please try again later.";
      } else {
        errorMessage = result["errors"]
          .map(function (error) {
            return error.message;
          })
          .join("<br>");
      }
      this.error(errorMessage);
    } else {
      this.error(
        "Something went wrong when creating the permission request, please try again later."
      );
    }
  }

  render() {
    return html`
      <div>
        <header>
          <h1>Connect to Mijn Aansluiting</h1>
        </header>

        <form @submit="${this.handleSubmit}">
          <sl-input
            label="House Number"
            type="text"
            helpText="This is the house number of your address. It is required by Mijn Aansluiting to verify that this is the correct metering point."
            name="verificationCode"
            placeholder="House Number"
            required
          ></sl-input>
          <p>
            By clicking on this button, you will access your personal Mijn
            Aansluiting account where you can authorise Mijn Aansluiting to send
            us your data.
          </p>

          <input id="submit" type="submit" value="Submit" />
        </form>
      </div>
    `;
  }
}

export default PermissionRequestForm;
