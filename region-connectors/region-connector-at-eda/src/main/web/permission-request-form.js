import { html, LitElement } from "lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/alert/alert.js";

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

    this._requestId = "";
    this._requestStatus = "";
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    formData.append("connectionId", this.connectionId);

    const startDate = new Date();
    startDate.setDate(
      startDate.getDate() + this.dataNeedAttributes.durationStart
    );

    let endDate;
    if (this.dataNeedAttributes.durationEnd === 0) {
      endDate = new Date(Date.now() - 24 * 60 * 60 * 1000);
    } else {
      endDate = new Date(startDate);
      endDate.setDate(endDate.getDate() + this.dataNeedAttributes.durationEnd);
    }

    formData.append("start", startDate.toISOString().substring(0, 10));
    formData.append("end", endDate.toISOString().substring(0, 10));

    fetch(REQUEST_URL, {
      body: formData,
      method: "POST",
    })
      .then((response) => response.json())
      .then((result) => {
        console.log(result);
        this._requestId = result["cmRequestId"];
        const permissionId = result["permissionId"];

        this.requestPermissionStatus(permissionId);
        // poll /permission-request?permissionId=... until the status is either "GRANTED" or "REJECTED"
        this.intervalId = setInterval(
          this.requestPermissionStatus(permissionId),
          5000
        );
      })
      .catch((error) => console.error(error));
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
          if (
            result["status"] === "ACCEPTED" ||
            result["status"] === "REJECTED" ||
            result["status"] === "INVALID" ||
            result["status"] === "TERMINATED"
          ) {
            clearInterval(this.intervalId);
          }

          this._requestStatus = result["status"];
        })
        .catch((error) => console.error(error));
    };
  }

  render() {
    return html`
      <div>
        <form @submit="${this.handleSubmit}">
          <sl-input
            label="Metering Point Number"
            type="text"
            name="meteringPointId"
            minlength="33"
            maxlength="33"
            required
          ></sl-input>

          <br />

          <div>
            <sl-button type="submit" variant="primary">Connect</sl-button>
          </div>
        </form>

        ${this._requestStatus &&
        html` <sl-alert open>
          <sl-icon slot="icon" name="info-circle"></sl-icon>

          <p>The CM request ID for this connection is: ${this._requestId}</p>
          <p>The request status is: ${this._requestStatus}</p>
        </sl-alert>`}
      </div>
    `;
  }
}

export default PermissionRequestForm;
