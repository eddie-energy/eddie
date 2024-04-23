import { html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";

class PermissionRequestFormBase extends LitElement {
  ERROR_TITLE = "An error occurred";
  MAX_RETRIES = 60; // Retry polling for 5 minutes

  restartPollingButtonRef = createRef();

  permissionId = null;
  location = null;

  awaitRetry(delay, maxRetries) {
    return new Promise((resolve) => setTimeout(resolve, delay)).then(() => {
      if (maxRetries > 0) {
        return this.requestPermissionStatus(this.location, maxRetries - 1);
      }

      // Handle the case when the maximum number of retries is reached
      const retryButton = html`
        <sl-button
          ref=${ref(this.restartPollingButtonRef)}
          variant="neutral"
          outline
          @click="${this.startOrRestartAutomaticPermissionStatusPolling}"
          >Restart polling
        </sl-button>
      `;

      const warningTitle = "Automatic query stopped.";
      const warningMessage =
        "Permission status query exceeded maximum allowed attempts.\n" +
        "Click the button below to restart the automatic polling.";
      this.notify({
        title: warningTitle,
        message: warningMessage,
        variant: "warning",
        extraFunctionality: [retryButton],
      });
    });
  }

  startOrRestartAutomaticPermissionStatusPolling = () => {
    if (this.restartPollingButtonRef.value) {
      const parent = this.restartPollingButtonRef.value.parentElement;
      this.restartPollingButtonRef.value.remove();
      parent.remove();
    }

    this.requestPermissionStatus(this.location, this.MAX_RETRIES)
      .then()
      .catch((error) => {
        this.notify({
          title: this.ERROR_TITLE,
          message: error,
          variant: "danger",
        });
      });
  };

  /**
   * Dispatch a custom event to render a user notification.
   * @param {Object} notification
   * @param {string} notification.title
   * @param {string} notification.message
   * @param {string} [notification.reason=""]
   * @param {string} [notification.variant="info"]
   * @param {number} [notification.duration=Infinity]
   * @param {string[]} [notification.extraFunctionality=[]]
   */
  notify({
    title,
    message,
    reason = "",
    variant = "info",
    duration = Infinity,
    extraFunctionality = [],
  }) {
    this.dispatchEvent(
      new CustomEvent("eddie-notification", {
        detail: {
          title,
          message,
          reason,
          variant,
          duration,
          extraFunctionality,
        },
        bubbles: true,
        composed: true,
      })
    );
  }

  handleStatus(status, reason = "") {
    this.dispatchEvent(
      new CustomEvent("eddie-request-status", {
        detail: {
          status,
          reason,
        },
        bubbles: true,
        composed: true,
      })
    );
  }
}

export default PermissionRequestFormBase;
