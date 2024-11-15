import { LitElement } from "lit";

const TERMINAL_STATES = [
  "TERMINATED",
  "REVOKED",
  "FULFILLED",
  "INVALID",
  "MALFORMED",
];

class PermissionRequestFormBase extends LitElement {
  constructor() {
    super();

    /**
     * URL of the core service inferred from the import URL.
     * @type {string}
     */
    this.CORE_URL = new URL(import.meta.url).origin;

    /**
     * Base URL of the current script inferred from the import URL.
     * @type {string}
     */
    this.BASE_URL = new URL(import.meta.url).href
      .replace("ce.js", "")
      .slice(0, -1);

    /**
     * Endpoint for sending permission requests inferred from the base URL.
     * @type {string}
     */
    this.REQUEST_URL = this.BASE_URL + "/permission-request";

    /**
     * Endpoint for subscribing to the status of a permission request.
     * @type {string}
     */
    this.REQUEST_STATUS_URL = this.CORE_URL + "/api/connection-status-messages";
  }

  /**
   * Prevents the permission request from skipping validation by waiting for
   * Shoelace input elements to load before adding the submit event listener.
   * See GH-899 for details.
   */
  firstUpdated(_) {
    customElements.whenDefined("sl-input").then(() => {
      this.shadowRoot
        .getElementById("request-form")
        ?.addEventListener("submit", this.handleSubmit.bind(this));
    });
  }

  /**
   * Automatically binds to the form with id `request-form`.
   * @param {SubmitEvent} event
   * @returns void
   */
  handleSubmit(event) {
    throw new Error("Must be implemented by subclass!");
  }

  /**
   * Dispatch a custom event to render an error notification.
   * @param {string} message Error details to display.
   * @param {number} duration Duration in milliseconds to display the notification for.
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
   * @param {string} notification.title Title of the notification.
   * @param {string} notification.message Notification details to display.
   * @param {string} notification.reason Optional reason for a notification.
   * @param {"info"|"success"|"warning"|"danger"} notification.variant Indicates the urgency of the notification.
   * @param {number} notification.duration Duration in milliseconds for which the notification is displayed.
   * @param {string[]|Node[]} notification.extraFunctionality Additional content to render at the end of the notification.
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

  /**
   * Create a standard permission request with the given payload.
   * Errors are thrown as an `Error` object with the message as the error message.
   * This includes status codes outside the 2xx range.
   * @param {any} payload The request body to send. Will be converted to a JSON string using `JSON.stringify`.
   * @param {RequestInit} options Additional options to pass to the fetch call.
   * @returns {Promise<any>} The response body as JSON.
   * @throws {Error} If the request fails or the response has a status code outside the 2xx range.
   */
  async createPermissionRequest(payload, options = {}) {
    const response = await fetch(this.REQUEST_URL, {
      body: JSON.stringify(payload),
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      ...options,
    });

    const data = await response.json();

    if (response.ok) {
      if (response.status === 201) {
        this.notify({
          title: "Permission request created!",
          message: "Your permission request was created successfully.",
          variant: "success",
          duration: 10000,
        });

        const { permissionId } = data;
        this.pollRequestStatus(`${this.REQUEST_STATUS_URL}/${permissionId}`);
      }

      return data;
    }

    const { errors } = data;

    if (errors && errors.length > 0) {
      const message = errors.map((error) => error.message).join(". ");
      throw new Error(message);
    }

    throw new Error(
      "Something went wrong when creating the permission request, please try again later."
    );
  }

  pollRequestStatus(location) {
    const eventSource = new EventSource(location);

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);

      this.dispatchEvent(
        new CustomEvent("eddie-request-status", {
          detail: data,
          bubbles: true,
          composed: true,
        })
      );

      const status = data.status.toLowerCase().replaceAll("_", "-");

      this.dispatchEvent(
        new Event(`eddie-request-${status}`, {
          bubbles: true,
          composed: true,
        })
      );

      if (TERMINAL_STATES.includes(data.status)) {
        eventSource.close();
      }
    };
  }
}

export default PermissionRequestFormBase;
