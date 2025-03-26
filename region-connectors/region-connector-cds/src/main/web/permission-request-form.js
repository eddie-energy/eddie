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
    _redirectUri: { type: String },
  };

  constructor() {
    super();
  }

  connectedCallback() {
    super.connectedCallback();
    this.addEventListener("eddie-request-status", (event) => {
      const { additionalInformation } = event.detail;
      if (additionalInformation && !this._redirectUri) {
        this._redirectUri = additionalInformation.redirectUri;
      }
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const payload = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
      cdsId: Number.parseInt(this.companyId),
    };
    this.createPermissionRequest(payload)
      .catch((error) => this.error(error))
      .then((data) => {
        this._redirectUri = data.redirectUri;
        window.open(this._redirectUri);
      });
  }

  render() {
    return !this._redirectUri
      ? html`
          <div>
            <form id="request-form">
              <p>
                By clicking on this button, you will access your personal
                utility account where you can authorize the utility to send us
                your data.
              </p>
              <sl-button
                type="submit"
                variant="primary"
                ?disabled="${this._redirectUri}"
                >Create
              </sl-button>
            </form>
          </div>
        `
      : html`
          <div>
            <p>
              If you have not been forwarded automatically, click on the link
              below.
            </p>
            <a href="${this._redirectUri}" target="_blank">
              ${new URL(this._redirectUri).host}
            </a>
          </div>
        `;
  }
}

export default PermissionRequestForm;
