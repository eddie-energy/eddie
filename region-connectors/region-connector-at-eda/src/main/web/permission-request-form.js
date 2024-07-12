import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    companyId: { attribute: "company-id" },
    accountingPointId: { attribute: "accounting-point-id" },
    _requestId: { type: String },
    _requestStatus: { type: String },
    _isSubmitDisabled: { type: Boolean },
  };

  constructor() {
    super();

    this._requestId = "";
    this._isSubmitDisabled = false;
  }

  connectedCallback() {
    super.connectedCallback();
    this.addEventListener("eddie-request-status", (event) => {
      const {
        additionalInformation: { cmRequestId },
        status,
      } = event.detail;

      this._requestId = cmRequestId ?? "";
      this._requestStatus = status;
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    const jsonData = {
      meteringPointId: formData.get("meteringPointId")
        ? formData.get("meteringPointId")
        : null,
      dsoId: this.companyId,
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(jsonData)
      .then(() => {
        this._requestStatus = "CREATED";
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        this.error(error);
      });
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-input
            label="Zählpunktnummer"
            type="text"
            .helpText=${this.accountingPointId
              ? "The service has already provided a Zählpunktnummer. If this value is incorrect, please contact the service provider."
              : "Enter your 33-character Zählpunktnummer for the request to show up in your DSO portal. Leave blank to search for the generated Consent Request ID."}
            name="meteringPointId"
            minlength="33"
            maxlength="33"
            placeholder="${this.companyId}..."
            .value="${this.accountingPointId
              ? this.accountingPointId
              : nothing}"
            .disabled="${!!this.accountingPointId}"
          ></sl-input>

          <br />

          <div>
            <sl-button
              type="submit"
              variant="primary"
              ?disabled="${this._isSubmitDisabled}"
              >Connect
            </sl-button>
          </div>
        </form>

        ${this._requestStatus === "CREATED"
          ? html`<br />
              <sl-alert open>
                <sl-icon slot="icon" name="info-circle"></sl-icon>

                <p>
                  Please wait while we are sending the permission request to the
                  permission administrator.
                </p>
              </sl-alert>`
          : ""}
        ${this._requestStatus === "SENT_TO_PERMISSION_ADMINISTRATOR"
          ? html`<br />
              <sl-alert open>
                <sl-icon slot="icon" name="info-circle"></sl-icon>

                <p>
                  The Consent Request ID for this connection is:
                  ${this._requestId}
                </p>

                <p>
                  Further steps are required at the website of the permission
                  administrator. Visit the website using the button below and
                  look for your provided Zählpunktnummer or the Consent Request
                  with ID ${this._requestId}.
                </p>

                ${this.jumpOffUrl
                  ? html` <sl-button href="${this.jumpOffUrl}" target="_blank">
                      Visit permission administrator website
                    </sl-button>`
                  : ""}
              </sl-alert>`
          : ""}
      </div>
    `;
  }
}

export default PermissionRequestForm;
