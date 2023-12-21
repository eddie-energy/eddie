import { html } from "lit";
import PermissionRequestFormBase from "../../../../../core/src/main/js/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    jumpOffUrl: { attribute: "jump-off-url" },
    companyId: { attribute: "company-id" },
    _requestId: { type: String },
    _requestStatus: { type: String },
  };

  constructor() {
    super();

    this._requestId = "";
    this._requestStatus = "";
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    let jsonData = {};

    if (formData.get("meteringPointId") !== "") {
      jsonData.meteringPointId = formData.get("meteringPointId");
    }

    const startDate = new Date();
    startDate.setDate(
      startDate.getDate() + this.dataNeedAttributes.durationStart
    );

    const endDate = new Date();
    if (this.dataNeedAttributes.durationEnd === 0) {
      endDate.setDate(endDate.getDate() - 1); // subtract one day by default
    } else {
      endDate.setDate(endDate.getDate() + this.dataNeedAttributes.durationEnd);
    }

    jsonData.dsoId = this.companyId;
    jsonData.connectionId = this.connectionId;
    jsonData.start = startDate.toISOString().substring(0, 10);
    jsonData.end = endDate.toISOString().substring(0, 10);
    jsonData.dataNeedId = this.dataNeedAttributes.id;
    jsonData.granularity = this.dataNeedAttributes.granularity;

    this.createPermissionRequest(jsonData)
      .then()
      .catch((error) =>
        this.notify(this.ERROR_TITLE, error, "danger", "exclamation-octagon")
      );
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
        const successTitle = "Permission request created!";
        const successMessage =
          "Your permission request was created successfully.";
        this.notify(
          successTitle,
          successMessage,
          "success",
          "check2-circle",
          "5000"
        );
      } else if (response.status === 400) {
        // An error on the client side happened, and it should be displayed as alert in the form
        let errorMessage;

        if (result["errors"] == null || result["errors"].length === 0) {
          errorMessage =
            "Something went wrong when creating the permission request, please try again later.";
        } else {
          errorMessage = result["errors"].join("<br>");
        }
        this.notify(
          this.ERROR_TITLE,
          errorMessage,
          "danger",
          "exclamation-octagon"
        );

        return;
      } else {
        const errorMessage =
          "Something went wrong when creating the permission request, please try again later.";
        this.notify(
          this.ERROR_TITLE,
          errorMessage,
          "danger",
          "exclamation-octagon"
        );

        return;
      }

      this._requestId = result["cmRequestId"];
      this.permissionId = result["permissionId"];
      this.startOrRestartAutomaticPermissionStatusPolling();
    } catch (e) {
      this.notify(this.ERROR_TITLE, e, "danger", "exclamation-octagon");
    }
  }

  async requestPermissionStatus(permissionId, maxRetries) {
    let response = await fetch(
      BASE_URL + "permission-status/" + permissionId
    );

    if (response.status === 404) {
      // No permission request was created
      this.notify(
        this.ERROR_TITLE,
        "Your permission request could not be created.",
        "danger",
        "exclamation-octagon"
      );
      return;
    }
    if (response.status !== 200) {
      // An unexpected status code was sent, try again in 10 seconds
      const millisecondsToWait = 10000;
      this.notify(
        this.ERROR_TITLE,
        "An unexpected error happened, trying again in " +
          millisecondsToWait / 1000 +
          " seconds",
        "danger",
        "exclamation-octagon",
        millisecondsToWait.toString()
      );
      await this.awaitRetry(millisecondsToWait, maxRetries);
      return;
    }

    const result = await response.json();
    const currentStatus = result["status"];
    this._requestStatus = currentStatus;

    // Finished long poll
    if (
      currentStatus === "ACCEPTED" ||
      currentStatus === "REJECTED" ||
      currentStatus === "INVALID" ||
      currentStatus === "TERMINATED" ||
      result["status"] === "FULFILLED"
    ) {
      const successTitle = "Finished!";
      const successMessage = "Your consumption record has been received.";
      this.notify(
        successTitle,
        successMessage,
        "success",
        "check2-circle",
        "5000"
      );

      return;
    }

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
            help-text="Enter your Zählpunktnummer for the request to show up in your DSO portal. Leave blank to search for the generated Consent Request ID."
            name="meteringPointId"
            minlength="33"
            maxlength="33"
            placeholder="${this.companyId}"
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

      <div id="${this.USER_NOTIFICATION_CONTAINER_ID}"></div>
    `;
  }
}

export default PermissionRequestForm;