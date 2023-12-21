import { html, LitElement } from "lit";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/icon/icon.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";
const MAX_RETRIES = 60; // Retry polling for 5 minutes
const ERROR_TITLE = "An error occurred";
const USER_NOTIFICATION_CONTAINER_ID = "user-notifications-container";
const RESTART_POLLING_BUTTON_ID = "restart-polling-button";

class PermissionRequestForm extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    _requestId: { type: String },
    _requestStatus: { type: String },
  };

  permissionId = null;

  constructor() {
    super();

    this._requestStatus = "";
    this._isSubmitDisabled = false;
  }

  awaitRetry(delay, maxRetries) {
    return new Promise((resolve) => setTimeout(resolve, delay)).then(() => {
      if (maxRetries > 0) {
        return this.requestPermissionStatus(this.permissionId, maxRetries - 1);
      } else {
        // Handle the case when the maximum number of retries is reached
        const retryButton = Object.assign(document.createElement("sl-button"), {
          id: RESTART_POLLING_BUTTON_ID,
          variant: "neutral",
          outline: true,
          innerHTML: "Restart polling",
          onclick: this.startOrRestartAutomaticPermissionStatusPolling,
        });

        const warningTitle = "Automatic query stopped.";
        const warningMessage =
          "Permission status query exceeded maximum allowed attempts.\n" +
          "Click the button below to restart the automatic polling.";
        this.notify(
          warningTitle,
          warningMessage,
          "warning",
          "exclamation-triangle",
          "Infinity",
          [retryButton]
        );
      }
    });
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

    let jsonData = {};
    jsonData.refreshToken = formData.get("refreshToken");
    jsonData.meteringPoint = formData.get("meteringPoint");
    jsonData.connectionId = this.connectionId;
    jsonData.granularity = this.dataNeedAttributes.granularity;
    jsonData.start = startDate.toISOString().substring(0, 10);
    jsonData.end = endDate.toISOString().substring(0, 10);
    jsonData.dataNeedId = this.dataNeedAttributes.id;
    jsonData.granularity = this.dataNeedAttributes.granularity;

    this.createPermissionRequest(jsonData)
      .then()
      .catch((error) =>
        this.notify(ERROR_TITLE, error, "danger", "exclamation-octagon")
      );
  }

  async createPermissionRequest(formData) {
    try {
      const response = await fetch(REQUEST_URL, {
        body: JSON.stringify(formData),
        method: "POST",
        headers: {
          "Content-Type": "application/json",
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
          "3000"
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

        this.notify(ERROR_TITLE, errorMessage, "danger", "exclamation-octagon");
        this._isSubmitDisabled = false;

        return;
      } else {
        const errorMessage =
          "Something went wrong when creating the permission request, please try again later.";
        this.notify(ERROR_TITLE, errorMessage, "danger", "exclamation-octagon");

        return;
      }

      this.permissionId = result["permissionId"];
      this.startOrRestartAutomaticPermissionStatusPolling();
    } catch (e) {
      this.notify(ERROR_TITLE, e, "danger", "exclamation-octagon");
    }
  }

  async requestPermissionStatus(permissionId, maxRetries) {
    const response = await fetch(
      BASE_URL + "permission-status/" + permissionId
    );

    if (response.status === 404) {
      // No permission request was created
      this.notify(
        ERROR_TITLE,
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
        ERROR_TITLE,
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
      currentStatus === "TERMINATED"
    ) {
      this._isSubmitDisabled = false;
      const successTitle = "Finished!";
      const successMessage = "Your consumption record has been received.";
      this.notify(
        successTitle,
        successMessage,
        "success",
        "check2-circle",
        "3000"
      );
      return;
    }

    // Disable Submit
    if (
      currentStatus === "SENT_TO_PERMISSION_ADMINISTRATOR" ||
      currentStatus === "RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE"
    ) {
      this._isSubmitDisabled = true;
    }

    // Wait for status update
    await this.awaitRetry(5000, maxRetries);
  }

  escapeHtml(title, message) {
    const div = this.shadowRoot.ownerDocument.createElement("div");
    div.innerHTML = "<p><strong>" + title + "</strong><br>" + message + "</p>";
    return div.innerHTML;
  }

  notify(
    title,
    message,
    variant = "primary",
    iconString = "info-circle",
    duration = "Infinity",
    extraFunctionality = []
  ) {
    const container = this.shadowRoot.getElementById(
      USER_NOTIFICATION_CONTAINER_ID
    );
    const icon = "<sl-icon name=" + iconString + ' slot="icon"></sl-icon>';

    const alert = Object.assign(document.createElement("sl-alert"), {
      variant: variant,
      duration: duration,
      closable: true,
      open: true,
      innerHTML: `
        ${icon}
        ${this.escapeHtml(title, message)}
      `,
    });

    extraFunctionality.forEach((element) => alert.append(element));
    container.append(alert);
  }

  startOrRestartAutomaticPermissionStatusPolling() {
    const restartPollingButton = this.shadowRoot.getElementById(
      RESTART_POLLING_BUTTON_ID
    );
    if (restartPollingButton != null) {
      const parent = restartPollingButton.parentElement;
      restartPollingButton.remove();
      parent.remove();
    }

    this.requestPermissionStatus(this.permissionId, MAX_RETRIES)
      .then()
      .catch((error) => {
        this.notify(ERROR_TITLE, error, "danger", "exclamation-octagon");
      });
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

        <div id=${USER_NOTIFICATION_CONTAINER_ID}></div>

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
