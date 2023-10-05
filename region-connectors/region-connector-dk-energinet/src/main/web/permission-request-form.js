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
    _isCooldown: { type: Boolean },
    _isSubmitHidden: { type: Boolean },
  };
  
  intervalId = null;
  
  constructor() {
    super();
    
    this._requestStatus = "";
    this._isSubmitDisabled = false;
    this._isSubmitHidden = false;
  }
  
  permissionId = null;

  handleSubmit(event) {
    this._isSubmitDisabled = true;
    event.preventDefault();
    
    const formData = new FormData(event.target);
    formData.append("connectionId", this.connectionId);
    formData.append("aggregation", this.dataNeedAttributes.granularity);
    
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

  render() {
    return html`
      <div>
        <section>
          <h2>Energinet - Eloverblik Customer</h2>
          <p>
            Eloverblik needs a refresh token in order to access your data. A
            refresh token can be generated in the DataHub.
          </p>
        </section>
        <form @submit="${this.handleSubmit}">
            <sl-input label="Refresh Token" type="text" id="refreshToken" name="refreshToken" required></sl-input>
          <br>
            <sl-input label="Metering Point" type="text" id="meteringPoint" name="meteringPoint" required></sl-input>
          <br>
          <sl-select label="Aggregation" value="Actual" name="aggregation" id="aggregation" required>
            <sl-option value="Actual">Actual</sl-option>
            <sl-option value="Quarter">Quarter</sl-option>
            <sl-option value="Hour">Hour</sl-option>
            <sl-option value="Day">Day</sl-option>
            <sl-option value="Month">Month</sl-option>
            <sl-option value="Year">Year</sl-option>
          </sl-select>
          <br>
          <sl-button
            .disabled="${this._isSubmitDisabled}"
            ?hidden="${this._isSubmitHidden}"
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
        </sl-alert>`}
      </div>
    `;
  }
}

export default PermissionRequestForm;
