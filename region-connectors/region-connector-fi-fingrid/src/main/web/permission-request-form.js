import { css, html, nothing } from "lit";
import { ifDefined } from "lit/directives/if-defined.js";
import { until } from "lit/directives/until.js";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/copy-button/copy-button.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static styles = css`
    dl {
      display: grid;
      grid-template-columns: auto 1fr;
      align-items: baseline;
    }

    dt {
      font-style: italic;
    }
  `;

  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    customerIdentification: { attribute: "customer-identification" },
    _isValidated: { type: Boolean },
    _isSubmitDisabled: { type: Boolean },
    _isVerifying: { type: Boolean },
  };

  permissionId = null;

  constructor() {
    super();
  }

  connectedCallback() {
    super.connectedCallback();

    this.addEventListener("eddie-request-validated", () => {
      this._isValidated = true;
    });
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

    this.createPermissionRequest(payload)
      .then(({ permissionId, accessToken }) => {
        this.permissionId = permissionId;
        this.accessToken = accessToken;

        this.fetchOrganisationData();
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        this.error(error);
      });
  }

  accepted() {
    fetch(`${this.REQUEST_URL}/${this.permissionId}/accepted`, {
      method: "PATCH",
      headers: {
        Authorization: "Bearer " + this.accessToken,
      },
    })
      .then(() => {
        this._isVerifying = true;
      })
      .catch((error) => {
        this._isVerifying = false;
        this.error(error);
      });
  }

  rejected() {
    fetch(`${this.REQUEST_URL}/${this.permissionId}/rejected`, {
      method: "PATCH",
      headers: {
        Authorization: "Bearer " + this.accessToken,
      },
    }).catch((error) => this.error(error));
  }

  fetchOrganisationData() {
    fetch(`${this.BASE_URL}/organisation-information`)
      .then((response) => response.json())
      .then(({ organisationName, organisationUser }) => {
        this._organisationUser = organisationUser;
        this._organisationName = organisationName;
      });
  }

  render() {
    return html`
      <form id="request-form" ?hidden="${this._isValidated}">
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
          .value="${ifDefined(this.customerIdentification)}"
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

      <div ?hidden="${!this._isValidated}">
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
          href="${this.jumpOffUrl}"
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
            ?disabled="${this._isVerifying}"
          >
            Accepted
          </sl-button>
          <sl-button
            variant="danger"
            @click="${this.rejected}"
            ?disabled="${this._isVerifying}"
          >
            Rejected
          </sl-button>
        </div>

        ${this._isVerifying
          ? html`
              <br />
              <sl-alert open>
                <sl-spinner slot="icon"></sl-spinner>

                We are verifying your permission with Fingrid. This might take a
                few minutes.
              </sl-alert>
            `
          : nothing}
      </div>
    `;
  }
}

export default PermissionRequestForm;
