import { html, LitElement, nothing } from "lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    jumpOffUrl: { attribute: "jump-off-url" },
    companyId: { attribute: "company-id" },
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

    if (formData.get("meteringPointId") === "") {
      formData.delete("meteringPointId");
      formData.append("dsoId", this.companyId);
    }
    formData.append("connectionId", this.connectionId);

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
      .then((response) => {
        if (!response.ok) {
          throw new Error("HTTP status " + response.status);
        }

        return response.json();
      })
      .then((result) => {
        this._requestId = result["cmRequestId"];
        const permissionId = result["permissionId"];

        this._requestStatus = "Request sent.";

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
            label="Zählpunktnummer"
            type="text"
            help-text="Enter your Zählpunktnummer for the request to show up in your DSO portal. Leave blank to search for the generated Consent Request ID."
            name="meteringPointId"
            minlength="33"
            maxlength="33"
            placeholder="${this.companyId}"
            required="${this.companyId ? nothing : true}"
          ></sl-input>

          <br />

          <div>
            <sl-button type="submit" variant="primary">Connect</sl-button>
          </div>
        </form>

        ${this._requestStatus &&
        html` <br />
          <sl-alert open>
            <sl-icon slot="icon" name="info-circle"></sl-icon>

            <p>The Consent Request ID for this connection is: ${this._requestId}</p>
            <p>The request status is: ${this._requestStatus}</p>

            <p>
              Further steps are required at the website of the permission
              administrator. Visit the website using the button below and look
              for your provided Zählpunktnummer or the Consent Request with ID
              ${this._requestId}.
            </p>

            ${this.jumpOffUrl
              ? html` <sl-button href="${this.jumpOffUrl}" target="_blank">
                  Visit permission administrator website
                </sl-button>`
              : ""}
          </sl-alert>`}
      </div>
    `;
  }
}

export default PermissionRequestForm;
