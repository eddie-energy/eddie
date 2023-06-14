import { html, LitElement, unsafeCSS } from "lit";
import shared from "../../../../../web/shared.css?inline";
import { createRef, ref } from "lit/directives/ref.js";

class PermissionRequestForm extends LitElement {
  static styles = unsafeCSS(shared);

  static properties = {
    messages: {},
  };

  cmRequestIdElement = createRef();
  requestStatusElement = createRef();
  intervalId = null;

  constructor() {
    super();
  }

  handleSubmit(event) {
    event.preventDefault();

    const url = new URL(import.meta.url).href.replace("ce.js", "");
    const requestUrl = url + "permission-request";

    const formData = new FormData(event.target);
    formData.append("connectionId", window.crypto.randomUUID());

    fetch(requestUrl, {
      body: formData,
      method: "POST",
    })
      .then((response) => response.json())
      .then((result) => {
        console.log(result);
        let cmRequestId = result["cmRequestId"];
        let permissionId = result["permissionId"];

        // get the cmRequestId element
        this.cmRequestIdElement.value.textContent =
          "CM Request ID: " + cmRequestId;
        this.cmRequestIdElement.value.hidden = false;

        this.requestPermissionStatus(url, permissionId);
        // poll /permission-request?permissionId=... until the status is either "GRANTED" or "REJECTED"
        this.intervalId = setInterval(
          this.requestPermissionStatus(url, permissionId),
          5000
        );
      })
      .catch((error) => console.error(error));
  }

  requestPermissionStatus(url, permissionId) {
    return () => {
      fetch(url + "permission-status?permissionId=" + permissionId)
        .then((response) => {
          if (!response.ok) {
            throw new Error("HTTP status " + response.status);
          }

          return response.json();
        })
        .then((result) => {
          if (
            result["status"] === "GRANTED" ||
            result["status"] === "REJECTED" ||
            result["status"] === "ERROR"
          ) {
            clearInterval(this.intervalId);
          }
          console.log("Status:" + result);
          this.requestStatusElement.value.textContent =
            "Request status: " + result["status"];
          this.requestStatusElement.value.hidden = false;
        })
        .catch((error) => console.error(error));
    };
  }

  render() {
    return html`
      <div>
        <header>
          <span class="title">EDA</span>
        </header>

        <form @submit="${this.handleSubmit}">
          <label for="meteringPointId">Metering Point ID:</label>
          <input
            type="text"
            id="meteringPointId"
            name="meteringPointId"
            min="33"
            max="33"
            required
          />

          <label for="start">Start:</label>
          <input type="date" id="start" name="start" required />

          <label for="end">End:</label>
          <input type="date" id="end" name="end" />

          <button type="submit">Connect</button>
        </form>

        <div ${ref(this.cmRequestIdElement)} hidden></div>
        <div ${ref(this.requestStatusElement)} hidden></div>
      </div>
    `;
  }
}

export default PermissionRequestForm;
