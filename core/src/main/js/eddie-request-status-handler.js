import { html, LitElement } from "lit";

import STATUS_MESSAGES from "./permission-process-status-messages.json";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";

const VARIANT_ICONS = {
  info: "info-circle",
  success: "check2-circle",
  warning: "exclamation-triangle",
  danger: "exclamation-octagon",
};

class EddieRequestStatusHandler extends LitElement {
  static properties = {
    status: { type: String },
    detailMessage: { type: String },
  };

  constructor() {
    super();

    this.addEventListener("eddie-request-status", (event) => {
      this.status = event.detail.status;
      this.detailMessage = event.detail.message;
    });
  }

  render() {
    if (!this.status) return html`<slot></slot>`;

    const { title, message, variant } = STATUS_MESSAGES[this.status];

    return html`
      <slot></slot>
      <br />
      <sl-alert variant="${variant}" open>
        <sl-icon name="${VARIANT_ICONS[variant]}" slot="icon"></sl-icon>
        <p>
          <strong>Status: ${title}</strong><br />
          ${message} ${this.detailMessage ?? ""}
        </p>
      </sl-alert>
    `;
  }
}

export default EddieRequestStatusHandler;
