import { html, nothing } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";

const BASE_URL = new URL(import.meta.url).href
  .replace("ce.js", "")
  .slice(0, -1);
const REQUEST_URL = BASE_URL + "/permission-request";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    jumpOffUrl: { attribute: "jump-off-url" },
    companyId: { attribute: "company-id" },
    accountingPointId: { attribute: "accounting-point-id" },
    _requestId: { type: String },
    _requestStatus: { type: String },
    _isSubmitDisabled: { type: Boolean },
  };

  constructor() {
    super();

    this._requestId = "";
    this._requestStatus = "";
    this._isSubmitDisabled = false;
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    const jsonData = {
      meteringPointId: formData.get("meteringPointId")
        ? formData.get("meteringPointId")
        : null,
      dsoId: this.companyId,
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedAttributes.id,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(jsonData)
      .then()
      .catch((error) =>
        this.notify({
          title: this.ERROR_TITLE,
          message: error,
          variant: "danger",
        })
      )
      .finally(() => {
        // request failed if no request status was set
        if (!this._requestStatus) {
          this._isSubmitDisabled = false
        }
      });
  }

  async createPermissionRequest(formData) {
    try {
      const response = await fetch(REQUEST_URL, {
        body: JSON.stringify(formData),
        method: "POST",
        headers: {
          "content-type": "application/json",
        },
      });
      const result = await response.json();

      if (response.status === 201) {
        const locationHeader = "Location";
        if (response.headers.has(locationHeader)) {
          this.location = BASE_URL + response.headers.get(locationHeader);
        } else {
          throw new Error("Header 'Location' is missing");
        }

        this.notify({
          title: "Permission request created!",
          message: "Your permission request was created successfully.",
          variant: "success",
          duration: 5000,
        });
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
        this.notify({
          title: this.ERROR_TITLE,
          message: errorMessage,
          variant: "danger",
        });

        return;
      } else {
        const errorMessage =
          "Something went wrong when creating the permission request, please try again later.";
        this.notify({
          title: this.ERROR_TITLE,
          message: errorMessage,
          variant: "danger",
        });

        return;
      }

      this._requestId = result["cmRequestId"];
      this.startOrRestartAutomaticPermissionStatusPolling();
    } catch (e) {
      this.notify({ title: this.ERROR_TITLE, message: e, variant: "danger" });
    }
  }

  async requestPermissionStatus(location, maxRetries) {
    let response = await fetch(location);

    if (response.status === 404) {
      // No permission request was created
      this.notify({
        title: this.ERROR_TITLE,
        message: "Your permission request could not be created.",
        variant: "danger",
      });
      return;
    }
    if (response.status !== 200) {
      // An unexpected status code was sent, try again in 10 seconds
      const millisecondsToWait = 10000;
      this.notify({
        title: this.ERROR_TITLE,
        message: `An unexpected error happened, trying again in ${
          millisecondsToWait / 1000
        } seconds`,
        variant: "danger",
        duration: millisecondsToWait,
      });
      await this.awaitRetry(millisecondsToWait, maxRetries);
      return;
    }

    const result = await response.json();
    const currentStatus = result["status"];
    this._requestStatus = currentStatus;

    this.handleStatus(currentStatus, result["message"]);

    // Wait for status update
    await this.awaitRetry(5000, maxRetries);
  }

  render() {
    return html`
      <div>
        <form @submit="${this.handleSubmit}">
          <sl-input
            label="Zählpunktnummer"
            type="text"
            .helpText=${this.accountingPointId
              ? "The service has already provided a Zählpunktnummer. If this value is incorrect, please contact the service provider."
              : "Enter your Zählpunktnummer for the request to show up in your DSO portal. Leave blank to search for the generated Consent Request ID."}
            name="meteringPointId"
            minlength="33"
            maxlength="33"
            placeholder="${this.companyId}"
            .value="${this.accountingPointId
              ? this.accountingPointId
              : nothing}"
            .disabled="${!!this.accountingPointId}"
          ></sl-input>

          <br />

          <div>
            <sl-button
              type="submit"
              variant="primary"
              ?disabled="${this._isSubmitDisabled}"
              >Connect</sl-button
            >
          </div>
        </form>

        ${this._isSubmitDisabled
          ? html`<br />
              <sl-alert open>
                <sl-icon slot="icon" name="info-circle"></sl-icon>
                <p>Your permission request is being processed.</p>
                <p>
                  Please wait for the request to finish. 
                  This process may take several minutes!
                </p>
              </sl-alert>`
          : ""}
        ${this._requestStatus &&
        html`<br />
          <sl-alert open>
            <sl-icon slot="icon" name="info-circle"></sl-icon>

            <p>
              The Consent Request ID for this connection is: ${this._requestId}
            </p>
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

      ${this.alerts}
    `;
  }
}

export default PermissionRequestForm;
