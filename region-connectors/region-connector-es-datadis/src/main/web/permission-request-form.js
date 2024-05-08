import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";

import logo from "../resources/datadis-logo.svg?raw";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

const BASE_URL = new URL(import.meta.url).href
  .replace("ce.js", "")
  .slice(0, -1);
const REQUEST_URL = BASE_URL + "/permission-request";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
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

    let jsonData = {
      connectionId: this.connectionId,
      meteringPointId: formData.get("meteringPointId"),
      nif: formData.get("nif"),
      dataNeedId: this.dataNeedAttributes.id,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(jsonData)
      .catch((error) => this.error(error))
      .finally(() => {
        if (!this._isPermissionRequestCreated) {
          this._isSubmitDisabled = false;
        }
      });
  }

  async createPermissionRequest(payload) {
    const response = await fetch(REQUEST_URL, {
      body: JSON.stringify(payload),
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    });
    const result = await response.json();

    if (response.status === 201) {
      const location = response.headers.get("Location");

      if (!location) {
        throw new Error("Header 'Location' is missing");
      }

      this.permissionId = result["permissionId"];

      this.handlePermissionRequestCreated(BASE_URL + location);
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

  accepted() {
    fetch(REQUEST_URL + `/${this.permissionId}/accepted`, {
      method: "PATCH",
      credentials: "include",
    })
      .then(() => {
        this._areResponseButtonsDisabled = true;
      })
      .catch((error) => this.error(error));
  }

  rejected() {
    fetch(REQUEST_URL + `/${this.permissionId}/rejected`, {
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
        <form @submit="${this.handleSubmit}">
          <sl-input
            label="DNI/Nif"
            type="text"
            id="nif"
            name="nif"
            required
          ></sl-input>

          <br />

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
