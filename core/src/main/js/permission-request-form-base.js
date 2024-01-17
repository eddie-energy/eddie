import { LitElement } from "lit";

class PermissionRequestFormBase extends LitElement {
  ERROR_TITLE = "An error occurred";
  USER_NOTIFICATION_CONTAINER_ID = "user-notifications-container";
  RESTART_POLLING_BUTTON_ID = "restart-polling-button";
  MAX_RETRIES = 60; // Retry polling for 5 minutes

  location = null;

  awaitRetry(delay, maxRetries) {
    return new Promise((resolve) => setTimeout(resolve, delay)).then(() => {
      if (maxRetries > 0) {
        return this.requestPermissionStatus(this.location, maxRetries - 1);
      } else {
        // Handle the case when the maximum number of retries is reached
        const retryButton = Object.assign(document.createElement("sl-button"), {
          id: this.RESTART_POLLING_BUTTON_ID,
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
      this.USER_NOTIFICATION_CONTAINER_ID
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

  startOrRestartAutomaticPermissionStatusPolling = () => {
    const restartPollingButton = this.shadowRoot.getElementById(
      this.RESTART_POLLING_BUTTON_ID
    );
    if (restartPollingButton != null) {
      const parent = restartPollingButton.parentElement;
      restartPollingButton.remove();
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
