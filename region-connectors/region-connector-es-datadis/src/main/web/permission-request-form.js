import { html, LitElement } from "lit";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";

import logo from "../resources/datadis-logo.svg?raw";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    _requestId: { type: String },
    _requestStatus: { type: String },
    _isCooldown: { type: Boolean },
    _isSubmitHidden: { type: Boolean },
    _areResponseButtonsDisabled: { type: Boolean },
  };

  intervalId = null;
  permissionId = null;

  constructor() {
    super();

    this._requestStatus = "";
    this._isSubmitDisabled = false;
    this._isSubmitHidden = false;
    this._areResponseButtonsDisabled = false;
  }

  isFormFilled(formData) {
    return !!(formData.get("meteringPointId") && formData.get("nif"));
  }

  handleSubmit(event) {
    this._isSubmitDisabled = true;
    event.preventDefault();

    const formData = new FormData(event.target);

    if (!this.isFormFilled(formData)) {
      return;
    }

    formData.append("connectionId", this.connectionId);
    formData.append("measurementType", "HOURLY");

    const startDate = new Date();
    startDate.setDate(
      startDate.getDate() + this.dataNeedAttributes.durationStart
    );

    let endDate = new Date();
    if (this.dataNeedAttributes.durationEnd === 0) {
      endDate.setDate(endDate.getDate() - 1); // subtract one day by default
    } else {
      endDate.setDate(endDate.getDate() + this.dataNeedAttributes.durationEnd);
    }

    formData.append(
      "requestDataFrom",
      startDate.toISOString().substring(0, 10)
    );
    formData.append("requestDataTo", endDate.toISOString().substring(0, 10));
    formData.append("dataNeedId", this.dataNeedAttributes.id);

    fetch(REQUEST_URL, {
      body: formData,
      method: "POST",
    })
      .then((response) => response.json())
      .then((result) => {
        this.permissionId = result["permissionId"];

        this.requestPermissionStatus(this.permissionId);
        this.intervalId = setInterval(
          this.requestPermissionStatus(this.permissionId),
          5000
        );
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        console.error(error);
      });
  }

  requestPermissionStatus(permissionId) {
    return () => {
      fetch(BASE_URL + "permission-status?permissionId=" + permissionId)
        .then((response) => {
          if (!response.ok) {
            throw new Error("HTTP status " + response.status);
          }

          return response.json();
        })
        .then((result) => {
          let currentStatus = result["status"];
          if (
            currentStatus === "ACCEPTED" ||
            currentStatus === "REJECTED" ||
            currentStatus === "INVALID" ||
            currentStatus === "TERMINATED"
          ) {
            clearInterval(this.intervalId);
          }
          if (
            currentStatus === "SENT_TO_PERMISSION_ADMINISTRATOR" ||
            currentStatus === "RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE"
          ) {
            this._isSubmitHidden = true;
          }

          this._requestStatus = currentStatus;
        })
        .catch((error) => console.error(error));
    };
  }

  accepted() {
    fetch(REQUEST_URL + "/accepted?permissionId=" + this.permissionId, {
      method: "POST",
    })
      .then((response) => {
        this._areResponseButtonsDisabled = true;
      })
      .catch((error) => console.error(error));
  }

  rejected() {
    fetch(REQUEST_URL + "/rejected?permissionId=" + this.permissionId, {
      method: "POST",
    })
      .then((response) => {
        this._areResponseButtonsDisabled = true;
      })
      .catch((error) => console.error(error));
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
            required
          ></sl-input>

          <br />

          <sl-button
            .disabled="${this._isSubmitDisabled}"
            ?hidden="${this._isSubmitHidden}"
            type="submit"
            variant="primary"
          >
            Connect
          </sl-button>
        </form>

        <div ?hidden="${!this._isSubmitHidden}">
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

        <br />

        ${this._requestStatus &&
        html` <sl-alert open>
          <sl-icon slot="icon" name="info-circle"></sl-icon>

          <p>The request status is: ${this._requestStatus}</p>
        </sl-alert>`}
      </div>
    `;
  }
}

export default PermissionRequestForm;
