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
    _requestId: { type: String },
    _requestStatus: { type: String },
  };

  constructor() {
    super();

    this._requestStatus = "";
  }

  handleSubmit(event) {
    event.preventDefault();

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
    jsonData.connectionId = this.connectionId;
    jsonData.start = startDate.toISOString().substring(0, 10);
    jsonData.end = endDate.toISOString().substring(0, 10);
    jsonData.dataNeedId = this.dataNeedAttributes.id;
    jsonData.granularity = this.dataNeedAttributes.granularity;

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

      this.startOrRestartAutomaticPermissionStatusPolling();
      window.open(result["redirectUri"], "_blank");
    } catch (e) {
      this.notify({ title: this.ERROR_TITLE, message: e, variant: "danger" });
    }
  }

  async requestPermissionStatus(location, maxRetries) {
    const response = await fetch(location);

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
    }

    // Wait for status update
    await this.awaitRetry(5000, maxRetries);
  }

  render() {
    return html`
      <div>
        <header>
          <h2>Who is ENEDIS?</h2>
          <p>
            Enedis manages the electricity network right up to the electricity
            meter. For our service, please authorise Enedis to send us your
            Linky data.
          </p>
          <p>
            Enedis manages the public electricity distribution network in 95% of
            mainland France.
          </p>
        </header>

        <form @submit="${this.handleSubmit}">
          <p>
            By clicking on this button, you will access your personal Enedis
            account where you can authorise Enedis to send us your data.
          </p>

          <label for="submit" style="cursor: pointer">
            <!-- temporary encoding of enedis action button as base64 -->
            <img
              alt="Share your ENEDIS Link data"
              src="data:image/png;base64,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"
            />
          </label>
          <input id="submit" type="submit" hidden />
        </form>

        ${this._requestStatus &&
        html` <sl-alert open>
          <sl-icon slot="icon" name="info-circle"></sl-icon>

          <p>The CM request ID for this connection is: ${this._requestId}</p>
          <p>The request status is: ${this._requestStatus}</p>
        </sl-alert>`}
      </div>

      ${this.alerts}
    `;
  }
}

export default PermissionRequestForm;
