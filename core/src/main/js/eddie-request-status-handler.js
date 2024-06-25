import { html, LitElement, render } from "lit";

import STATUS_MESSAGES from "./permission-process-status-messages.json";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

const VARIANT_ICONS = {
  info: "info-circle",
  success: "check2-circle",
  warning: "exclamation-triangle",
  danger: "exclamation-octagon",
};

const POLL_STATES = [
  "CREATED",
  "VALIDATED",
  "SENT_TO_PERMISSION_ADMINISTRATOR",
  "ACCEPTED",
];
const POLL_DELAY = 5000;
const RETRY_DELAY = 5000;
const MAX_RETRIES = 10;

class EddieRequestStatusHandler extends LitElement {
  connectedCallback() {
    this.addEventListener("eddie-request-created", this.handleRequestCreated);
  }

  disconnectedCallback() {
    this.removeEventListener(
      "eddie-request-created",
      this.handleRequestCreated
    );
  }

  handleRequestCreated(event) {
    this.dispatchEvent(
      new CustomEvent("eddie-notification", {
        detail: {
          title: "Permission request created!",
          message: "Your permission request was created successfully.",
          variant: "success",
          duration: 10000,
        },
        bubbles: true,
        composed: true,
      })
    );

    this.status = "CREATED";
    this.pollRequestStatus(event.detail.location);
  }

  async pollRequestStatus(location) {
    let triesLeft = MAX_RETRIES;

    while (triesLeft > 0) {
      try {
        const response = await fetch(location);

        if (!response.ok) {
          throw new Error("Failed to poll request status");
        }

        const { status, message, additionalInformation } =
          await response.json();

        if (this.status !== status) {
          this.status = status;
          this.renderStatus(status, message);
          this.dispatchEvent(
            new CustomEvent("eddie-request-status", {
              detail: {
                status: status,
                message: message,
                additionalInformation: additionalInformation,
              },
              bubbles: true,
              composed: true,
            })
          );

          if (!POLL_STATES.includes(status)) {
            break;
          }
        }

        await new Promise((resolve) => setTimeout(resolve, POLL_DELAY));
      } catch (error) {
        console.error(error);
        triesLeft--;
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY));
      }
    }

    if (triesLeft === 0) {
      this.renderRetryButton(location);
    }
  }

  renderRetryButton(location) {
    const button = document.createElement("sl-button");

    button.setAttribute("variant", "neutral");
    button.toggleAttribute("outline");
    button.setAttribute("data-location", location);
    button.textContent = "Restart polling";

    button.addEventListener("click", this.handleRetry.bind(this));

    this.dispatchEvent(
      new CustomEvent("eddie-notification", {
        detail: {
          title: "Automatic query stopped.",
          message:
            "Permission status query exceeded maximum allowed attempts. Click the button below to restart the automatic polling.",
          variant: "warning",
          extraFunctionality: [button],
        },
        bubbles: true,
        composed: true,
      })
    );
  }

  handleRetry(event) {
    const location = event.target.dataset.location;
    this.pollRequestStatus(location);
    event.target.closest("sl-alert").hide();
  }

  renderStatus(status, reason) {
    const { title, message, variant } = STATUS_MESSAGES[status];

    const element = html`
      <br />
      <sl-alert variant="${variant}" open>
        <sl-icon name="${VARIANT_ICONS[variant]}" slot="icon"></sl-icon>
        <p>
          <strong>Status: ${title}</strong><br />
          ${message} ${reason ? " " + reason : ""}
        </p>
      </sl-alert>
    `;

    render(element, this);
  }
}

export default EddieRequestStatusHandler;
