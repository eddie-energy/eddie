import { html } from "lit";

import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

// Shoelace components used by the form and status UI
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/spinner/spinner.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/components/icon/icon.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
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
      dataNeedId: this.dataNeedId,
    };

    this.createPermissionRequest(payload).catch((error) => this.error(error));
  }

  render() {
    const paName = this.companyName ?? "the Permission Administrator";

    // Before the request is sent to the PA, show instructions and a Connect button
    if (this._requestStatus !== "SENT_TO_PERMISSION_ADMINISTRATOR") {
      return html`
        <div>
          <form id="request-form">
            <sl-alert open>
              <sl-icon slot="icon" name="info-circle"></sl-icon>
              <p>
                To request data via DE-ETA, we will create a permission request
                linked to your connection. No additional information is required
                at this step.
              </p>
              <p>
                After creation, you may need to continue on ${paName}'s portal
                to confirm the request.
              </p>
              ${this.jumpOffUrl
                ? html`<p>
                    For more details, visit the ${paName} website:
                    <a href="${this.jumpOffUrl}" target="_blank"
                      >${this.jumpOffUrl}</a
                    >
                  </p>`
                : ""}
            </sl-alert>

            <br />

            <sl-button type="submit" variant="primary">Connect</sl-button>
          </form>
        </div>

        ${this._requestStatus === "CREATED" || this._requestStatus === "VALIDATED"
          ? html`<br />
              <sl-alert open>
                <sl-spinner slot="icon"></sl-spinner>
                <p>
                  Your permission request was created successfully. Please wait
                  while we are sending the permission request to the permission
                  administrator.
                </p>
              </sl-alert>`
          : ""}
      `;
    }

    // After submission to PA
    return html`
      <sl-alert open>
        <sl-icon slot="icon" name="info-circle"></sl-icon>
        <p>
          Your request was successfully sent to the permission administrator.
          Please continue on ${paName}'s website to complete the process.
        </p>
        ${this.jumpOffUrl
          ? html`<sl-button href="${this.jumpOffUrl}" target="_blank" variant="primary"
              >Continue to ${paName}</sl-button
            >`
          : ""}
      </sl-alert>
    `;
  }
}

export default PermissionRequestForm;
