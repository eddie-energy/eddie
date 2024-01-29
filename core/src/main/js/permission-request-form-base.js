import { html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";

const VARIANT_ICONS = {
  info: "info-circle",
  success: "check2-circle",
  warning: "exclamation-triangle",
  danger: "exclamation-octagon",
};

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
        this.notify({
          title: warningTitle,
          message: warningMessage,
          variant: "warning",
          extraFunctionality: [retryButton],
        });
      }
    });
  }

  notify({
    title,
    message,
    reason = "",
    variant = "info",
    duration = Infinity,
    extraFunctionality = [],
  }) {
    const alert = html`<sl-alert
        variant="${variant}"
        duration="${duration}"
        closable
        open
      >
        <sl-icon name="${VARIANT_ICONS[variant]}" slot="icon"></sl-icon>
        <p>
          <strong>${title}</strong><br />
          ${message}${reason && " Reason: " + reason}
        </p>
        ${extraFunctionality.map((element) => element)}
      </sl-alert>
      <br />`;
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
        this.notify({
          title: this.ERROR_TITLE,
          message: error,
          variant: "danger",
        });
      });
  };
}

export default PermissionRequestFormBase;
