import { html, render } from "lit";

const VARIANT_ICONS = {
  info: "info-circle",
  success: "check2-circle",
  warning: "exclamation-triangle",
  danger: "exclamation-octagon",
};

const REQUEST_STATUS_MESSAGES = {
  ACCEPTED: {
    title: "Request completed!",
    message: "Your permission request was accepted.",
    variant: "success",
    duration: 5000,
  },
  REJECTED: {
    title: "Request completed!",
    message: "The permission request has been rejected.",
  },
  INVALID: {
    title: "Request completed!",
    message: "The permission request was invalid.",
    variant: "warning",
  },
  TERMINATED: {
    title: "Request completed!",
    message: "The permission request was terminated.",
    variant: "warning",
  },
  FULFILLED: {
    title: "Request completed!",
    message: "The permission request was fulfilled.",
    variant: "success",
    duration: 5000,
  },
};

class EddieNotificationHandler extends HTMLElement {
  connectedCallback() {
    this.addEventListener("eddie-notification", this.handleNotification);
    this.addEventListener("eddie-request-status", this.handleRequestStatus);
  }

  disconnectedCallback() {
    this.removeEventListener("eddie-notification", this.handleNotification);
    this.removeEventListener("eddie-request-status", this.handleRequestStatus);
  }

  handleNotification(event) {
    this.renderNotification(event.detail);
  }

  renderNotification({
    title,
    message,
    reason = "",
    variant = "info",
    duration = Infinity,
    extraFunctionality = [],
  }) {
    const alert = html`
      <sl-alert
        variant="${variant}"
        duration="${duration}"
        closable
        open
        style="margin-top: var(--sl-spacing-medium)"
      >
        <sl-icon name="${VARIANT_ICONS[variant]}" slot="icon"></sl-icon>
        <p>
          <strong>${title}</strong><br />
          ${message}${reason && " Reason: " + reason}
        </p>
        ${extraFunctionality.map((element) => element)}
      </sl-alert>
    `;
    render(alert, this);
  }

  handleRequestStatus(event) {
    const { status, reason } = event.detail;

    let message = REQUEST_STATUS_MESSAGES[status];

    if (message) {
      message.reason = reason;
      this.renderNotification(message);
    } else {
      console.error(`Unknown permission status: ${status}`);
    }
  }
}

export default EddieNotificationHandler;
