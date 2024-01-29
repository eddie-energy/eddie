import { html } from "lit";
import PermissionRequestFormBase from "../../../../../core/src/main/js/permission-request-form-base.js";

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

    const jsonData = {
      meteringPointId: formData.get("meteringPointId")
        ? formData.get("meteringPointId")
        : null,
      dsoId: this.companyId,
      connectionId: this.connectionId,
      start: startDate.toISOString().substring(0, 10),
      end: endDate.toISOString().substring(0, 10),
      dataNeedId: this.dataNeedAttributes.id,
      granularity: this.dataNeedAttributes.granularity,
    };

    this.createPermissionRequest(jsonData)
      .then()
      .catch((error) =>
        this.notify({
          title: this.ERROR_TITLE,
          message: error,
          variant: "danger",
        })
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

    const title = "Finished!";
    // Finished long poll
    switch (currentStatus) {
      case "ACCEPTED":
        this.notify({
          title,
          message: "Your permission request was accepted.",
          variant: "success",
          duration: 5000,
        });
        return;
      case "REJECTED":
        this.notify({
          title,
          message: "The permission request has been rejected.",
          reason: result["message"],
        });
        return;
      case "INVALID":
        this.notify({
          title,
          message: "The permission request was invalid.",
          reason: result["message"],
          variant: "warning",
        });
        return;
      case "TERMINATED":
        this.notify({
          title,
          message: "The permission request was terminated.",
          reason: result["message"],
          variant: "warning",
        });
        return;
      case "FULFILLED":
        message = "The permission request was fulfilled.";
        this.notify(title, message, "success", "check2-circle", "5000");
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

      ${this.alerts}
    `;
  }
}

export default PermissionRequestForm;
