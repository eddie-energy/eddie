import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/radio-group/radio-group.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/radio-button/radio-button.js";

const shortUrlPrefix = "https://mijn.fluvius.be/verbruik/dienstverlener?id=";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    _requestStatus: { type: String },
    _shortUrlIdentifier: { type: String },
  };

  constructor() {
    super();
    this.addEventListener("eddie-request-status", (event) => {
      const {
        additionalInformation: { shortUrlIdentifier },
      } = event.detail;
      this._shortUrlIdentifier = shortUrlIdentifier;
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    const payload = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
      flow: formData.get("flow"),
    };

    this.createPermissionRequest(payload)
      .then((result) => {
        this._permissionId = result.permissionId;
      })
      .catch((error) => this.error(error));
  }

  render() {
    return html`
      <div>
        <header>
          <h2>Fluvius - Belgium Region Connector</h2>
        </header>

        <form id="request-form">
          <sl-radio-group label="Your type: " name="flow" value="B2C">
            <sl-radio-button value="B2B">B2B</sl-radio-button>
            <sl-radio-button value="B2C">B2C</sl-radio-button>
          </sl-radio-group>

          <br />

          <sl-button type="submit" variant="primary">Create</sl-button>
        </form>
      </div>
      <div ?hidden="${!this._shortUrlIdentifier}">
        <p>Please accept the authorization request in your Fluvius portal.</p>
        <a href="${shortUrlPrefix + this._shortUrlIdentifier}" target="_blank">
          Go to Fluvius
        </a>
      </div>
    `;
  }
}

export default PermissionRequestForm;
