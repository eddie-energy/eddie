import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    customerIdentification: { attribute: "customer-identification" },
    _isPermissionRequestCreated: { type: Boolean },
    _isSubmitDisabled: { type: Boolean },
    _areResponseButtonsDisabled: { type: Boolean },
    _organisationUser: { type: String },
    _organisationName: { type: String },
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
      customerIdentification: formData.get("customerIdentification"),
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

  connectedCallback() {
    super.connectedCallback();
    this.organisationData();
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

  organisationData() {
    fetch(this.BASE_URL + `/organisation-information`)
      .then((response) => response.json())
      .then((body) => {
        this._organisationUser = body.organisationUser;
        this._organisationName = body.organisationName;
      });
  }

  render() {
    return html`
      <div>
        <form id="request-form">
          <sl-input
            label="Customer Identification"
            id="customerIdentification"
            type="text"
            name="customerIdentification"
            .helpText=${
              this.customerIdentification
                ? "The service has already provided the customer identification. If this value is incorrect, please contact the service provider."
                : nothing
            }
            .value="${
              this.customerIdentification
                ? this.customerIdentification
                : nothing
            }"
            .disabled="${!!this.customerIdentification}"
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
          <p>
            Please create a permission request for this organization
          <div>
            <span id="org-user">${this._organisationUser}</span>
            <sl-copy-button from="org-user"></sl-copy-button>
          </div>
          <div>
            <span id="org-name">${this._organisationName}</span>
            <sl-copy-button from="org-name"></sl-copy-button>
          </div>
          </p>
          <a
            href="https://oma.fingrid.fi"
            target="_blank"
            style="display: inline-block; border-radius: 2em; padding: 0.5em 1em 0.5em 1em"
          >
            Create permission request
          </a>
          <p>
            Please let us know once you have created the authorization request
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
