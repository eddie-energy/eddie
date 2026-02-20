import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/icon/icon.js";

/**
 * Custom web component for the German (DE) ETA Plus permission request form.
 * This component provides a user-friendly interface for creating permission requests
 * through the EDDIE Demo Button.
 *
 * Follows the EDDIE framework pattern by extending PermissionRequestFormBase.
 */
class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    accountingPointId: { attribute: "accounting-point-id" },
    _requestId: { type: String },
    _isPermissionRequestCreated: { type: Boolean },
    _isSubmitDisabled: { type: Boolean },
  };

  constructor() {
    super();
    this._isPermissionRequestCreated = false;
    this._isSubmitDisabled = false;
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    const jsonData = {};
    jsonData.meteringPointId = formData.get("meteringPointId");
    jsonData.connectionId = this.connectionId;
    jsonData.dataNeedId = this.dataNeedId;

    this._isSubmitDisabled = true;

    this.createPermissionRequest(jsonData)
      .then((response) => {
        this._isPermissionRequestCreated = true;
        // Open OAuth authorization URL in new window
        if (response.redirectUri) {
          window.open(response.redirectUri, "_blank");
        }
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
            label="Metering Point ID"
            type="text"
            id="meteringPointId"
            name="meteringPointId"
            .helpText=${this.accountingPointId
        ? "The service has already provided a metering point. If this value is incorrect, please contact the service provider."
        : "Enter your German metering point identification number (e.g., DE0123456789...)"}
            .value="${this.accountingPointId
        ? this.accountingPointId
        : nothing}"
            .disabled="${!!this.accountingPointId}"
            placeholder="DE0123456789012345678901234567890"
            required
          ></sl-input>

          <br />

          <sl-button
            ?disabled="${this._isSubmitDisabled}"
            type="submit"
            variant="primary"
          >
            Connect to ETA Plus
          </sl-button>
        </form>
      </div>
    `;
  }
}

export default PermissionRequestForm;
