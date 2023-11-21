import { html, LitElement } from "lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/icon/icon.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    _requestId: { type: String },
    _requestStatus: { type: String },
  };

  intervalId = null;

  constructor() {
    super();

    this._requestStatus = "";
    this._isSubmitDisabled = false;
  }

  permissionId = null;
  error = null;

  handleSubmit(event) {
    this._isSubmitDisabled = true;
    event.preventDefault();

    const formData = new FormData(event.target);
    formData.append("connectionId", this.connectionId);
    formData.append("periodResolution", this.dataNeedAttributes.granularity);

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

    formData.append("start", startDate.toISOString().substring(0, 10));
    formData.append("end", endDate.toISOString().substring(0, 10));
    formData.append("dataNeedId", this.dataNeedAttributes.id);

    fetch(REQUEST_URL, {
      body: formData,
      method: "POST",
    })
      .then((response) => response.json())
      .then((result) => {
        this.permissionId = result["permissionId"];
        this.error = result["error"];

        this.requestPermissionStatus(this.permissionId);
        this.intervalId = setInterval(
          this.requestPermissionStatus(this.permissionId),
          500
        );
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        console.error(error);
      });
  }

  requestPermissionStatus(permissionId) {
    return () => {
      fetch(`${BASE_URL}permission-status?permissionId=${permissionId}`)
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
            currentStatus === "UNABLE_TO_SEND" ||
            currentStatus === "REJECTED" ||
            currentStatus === "INVALID" ||
            currentStatus === "TERMINATED"
          ) {
            clearInterval(this.intervalId);
          }

          this._requestStatus = currentStatus;
        })
        .catch((error) => console.error(error));
    };
  }

  render() {
    return html`
      <div>
        <form @submit="${this.handleSubmit}">
          <sl-input
            label="Refresh Token"
            type="text"
            id="refreshToken"
            name="refreshToken"
            help-text="Eloverblik needs a refresh token in order to access your data. A refresh token can be generated in the DataHub."
            required
          ></sl-input>
          <br />
          <sl-input
            label="Metering Point"
            type="text"
            id="meteringPoint"
            name="meteringPoint"
            required
          ></sl-input>
          <br />
          <sl-button
            .disabled="${this._isSubmitDisabled}"
            type="submit"
            variant="primary"
          >
            Connect
          </sl-button>
        </form>

        <br />

        ${this._requestStatus &&
        html` <sl-alert open>
          <sl-icon slot="icon" name="info-circle"></sl-icon>

          <p>The request status is: ${this._requestStatus}</p>
          <p>${this.error}</p>
        </sl-alert>`}
      </div>
    `;
  }
}

export default PermissionRequestForm;
