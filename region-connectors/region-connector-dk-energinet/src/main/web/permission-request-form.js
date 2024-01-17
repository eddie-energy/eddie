import { html } from "lit";
import PermissionRequestFormBase from "../../../../../core/src/main/js/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/icon/icon.js";

const BASE_URL = new URL(import.meta.url).href
  .replace("ce.js", "")
  .slice(0, -1);
const REQUEST_URL = BASE_URL + "/permission-request";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    _requestId: { type: String },
    _requestStatus: { type: String },
  };

  constructor() {
    super();

    this._requestStatus = "";
    this._isSubmitDisabled = false;
  }

  isFormFilled(formData) {
    return !!(formData.get("refreshToken") && formData.get("meteringPoint"));
  }

  handleSubmit(event) {
    this._isSubmitDisabled = true;
    event.preventDefault();

    const formData = new FormData(event.target);

    if (!this.isFormFilled(formData)) {
      return;
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

    const jsonData = {};
    jsonData.refreshToken = formData.get("refreshToken");
    jsonData.meteringPoint = formData.get("meteringPoint");
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

  async createPermissionRequest(payload) {
    try {
      const response = await fetch(REQUEST_URL, {
        body: JSON.stringify(payload),
        method: "POST",
        headers: {
          "Content-Type": "application/json",
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
          errorMessage = result["errors"]
            .map(function (error) {
              return error.message;
            })
            .join("<br>");
        }

        this.notify(
          this.ERROR_TITLE,
          errorMessage,
          "danger",
          "exclamation-octagon"
        );
        this._isSubmitDisabled = false;

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

      this.startOrRestartAutomaticPermissionStatusPolling();
    } catch (e) {
      this.notify(this.ERROR_TITLE, e, "danger", "exclamation-octagon");
    }
  }

  async requestPermissionStatus(location, maxRetries) {
    const response = await fetch(location);

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

    const title = "Finished!";
    let message = "";
    // Finished long poll
    switch (currentStatus) {
      case "SENT_TO_PERMISSION_ADMINISTRATOR":
        this._isSubmitDisabled = true;
        break;
      case "RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE":
        this._isSubmitDisabled = true;
        break;
      case "ACCEPTED":
        message = "Your permission request was accepted.";
        this.notify(title, message, "success", "check2-circle", "5000");
        return;
      case "REJECTED":
        message =
          "The permission request has been rejected. (Reason: " +
          result["message"] +
          ")";
        this.notify(title, message);
        return;
      case "INVALID":
        message =
          "The permission request was invalid. (Reason: " +
          result["message"] +
          ")";
        this.notify(title, message, "warning", "exclamation-triangle");
        return;
      case "TERMINATED":
        message =
          "The permission request was terminated. (Reason: " +
          result["message"] +
          ")";
        this.notify(title, message, "warning", "exclamation-triangle");
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

        <div id=${this.USER_NOTIFICATION_CONTAINER_ID}></div>

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
