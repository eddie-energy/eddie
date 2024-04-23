import { html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";

class PermissionRequestFormBase extends LitElement {
  /**
   * Dispatch a custom event to render an error notification.
   * @param {string} message
   * @param {number} duration
   */
  error(message, duration = Infinity) {
    this.notify({
      title: "An error occurred",
      message,
      variant: "danger",
      duration,
    });
  }

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
    const event = new CustomEvent("eddie-notification", {
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
    });

    this.dispatchEvent(event);
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
