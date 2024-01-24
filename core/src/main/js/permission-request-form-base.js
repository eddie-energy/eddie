import { html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";

class PermissionRequestFormBase extends LitElement {
  ERROR_TITLE = "An error occurred";
  MAX_RETRIES = 60; // Retry polling for 5 minutes

  static properties = {
    alerts: { type: Array },
  };

  restartPollingButtonRef = createRef();

  constructor() {
    super();
    this.alerts = [];
  }

  permissionId = null;
  location = null;

  awaitRetry(delay, maxRetries) {
    return new Promise((resolve) => setTimeout(resolve, delay)).then(() => {
      if (maxRetries > 0) {
        return this.requestPermissionStatus(this.location, maxRetries - 1);
      } else {
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

  notify(
    title,
    message,
    variant = "primary",
    iconString = "info-circle",
    duration = "Infinity",
    extraFunctionality = []
  ) {
    const alert = html`<sl-alert
        variant="${variant}"
        duration="${duration}"
        closable
        open
      >
        <sl-icon name="${iconString}" slot="icon"></sl-icon>
        <p><strong>${title}</strong><br />${message}</p>
        ${extraFunctionality.map((element) => element)} </sl-alert
      ><br />`;
    this.alerts.push(alert);
    this.requestUpdate();
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
        this.notify(this.ERROR_TITLE, error, "danger", "exclamation-octagon");
      });
  };
}

export default PermissionRequestFormBase;
