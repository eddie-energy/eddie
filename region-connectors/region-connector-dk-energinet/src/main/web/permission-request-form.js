import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/icon/icon.js";

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
    jsonData.refreshToken = formData.get("refreshToken");
    jsonData.meteringPoint = formData.get("meteringPoint");
    jsonData.connectionId = this.connectionId;
    jsonData.dataNeedId = this.dataNeedId;

    this._isSubmitDisabled = true;

    this.createPermissionRequest(jsonData)
      .then(() => {
        this._isPermissionRequestCreated = true;
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
            label="Metering Point"
            type="text"
            id="meteringPoint"
            name="meteringPoint"
            .helpText=${this.accountingPointId
              ? "The service has already provided a metering point. If this value is incorrect, please contact the service provider."
              : nothing}
            .value="${this.accountingPointId
              ? this.accountingPointId
              : nothing}"
            .disabled="${!!this.accountingPointId}"
            required
          ></sl-input>

          <br />

          <sl-input
            label="Refresh Token"
            type="text"
            id="refreshToken"
            name="refreshToken"
            required
          >
            <span slot="help-text">
              Eloverblik needs a refresh token in order to access your data. A
              refresh token can be generated in the DataHub. For more
              information see:
              <a
                target="_blank"
                href="https://energinet.dk/media/cxoho0xr/deling-af-egne-data-via-token.pdf"
              >
                https://energinet.dk/media/cxoho0xr/deling-af-egne-data-via-token.pdf
              </a>
            </span>
          </sl-input>

          <br />

          <sl-button
            ?disabled="${this._isSubmitDisabled}"
            type="submit"
            variant="primary"
          >
            Connect
          </sl-button>
        </form>
      </div>
    `;
  }
}

export default PermissionRequestForm;
