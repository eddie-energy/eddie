import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";

import logo from "../resources/datadis-logo.svg?raw";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    accountingPointId: { attribute: "accounting-point-id" },
    _isPermissionRequestCreated: { type: Boolean },
    _isSubmitDisabled: { type: Boolean },
    _areResponseButtonsDisabled: { type: Boolean },
  };

  permissionId = null;

  constructor() {
    super();

    this._isPermissionRequestCreated = false;
    this._isSubmitDisabled = false;
    this._areResponseButtonsDisabled = false;
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    let payload = {
      connectionId: this.connectionId,
      meteringPointId: formData.get("meteringPointId"),
      nif: formData.get("nif"),
      dataNeedId: this.dataNeedId,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(payload, {
      credentials: "include",
    })
      .then((result) => {
        this._isPermissionRequestCreated = true;
        this.permissionId = result["permissionId"];
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        this.error(error);
      });
  }

  accepted() {
    fetch(this.REQUEST_URL + `/${this.permissionId}/accepted`, {
      method: "PATCH",
      credentials: "include",
    })
      .then(() => {
        this._areResponseButtonsDisabled = true;
      })
      .catch((error) => this.error(error));
  }

  rejected() {
    fetch(this.REQUEST_URL + `/${this.permissionId}/rejected`, {
      method: "PATCH",
      credentials: "include",
    })
      .then(() => {
        this._areResponseButtonsDisabled = true;
      })
      .catch((error) => this.error(error));
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-input
            label="CUPS"
            id="meteringPointId"
            type="text"
            name="meteringPointId"
            .helpText=${this.accountingPointId
              ? "The service has already provided a CUPS value. If this value is incorrect, please contact the service provider."
              : nothing}
            .value="${this.accountingPointId
              ? this.accountingPointId
              : nothing}"
            .disabled="${!!this.accountingPointId}"
            required
          ></sl-input>

          <br />
          
          <sl-input
            label="DNI/NIF"
            type="text"
            id="nif"
            name="nif"
            placeholder="25744101M"
            help-text="We require the identification number you use to log into the Datadis web portal to request permission."
            required
          ></sl-input>

          <br />

          <sl-button
            ?disabled="${this._isSubmitDisabled}"
            type="submit"
            variant="primary"
          >
            Connect
          </sl-button>
        </form>

        <div ?hidden="${!this._isPermissionRequestCreated}">
          <p>Please accept the authorization request in your Datadis portal.</p>
          <a
            href="https://datadis.es/authorized-users"
            target="_blank"
            style="display: inline-block; background: #5D208B; border-radius: 2em; padding: 0.5em 1em 0.5em 1em"
          >
            ${unsafeSVG(logo)}
          </a>
          <p>
            Please let us know once you have accepted / rejected the
            authorization request
          </p>
          <div>
            <sl-button
              variant="success"
              @click="${this.accepted}"
              ?disabled="${this._areResponseButtonsDisabled}"
            >
              Accepted
            </sl-button>
            <sl-button
              variant="danger"
              @click="${this.rejected}"
              ?disabled="${this._areResponseButtonsDisabled}"
            >
              Rejected
            </sl-button>
          </div>
        </div>
      </div>
    `;
  }
}

export default PermissionRequestForm;
