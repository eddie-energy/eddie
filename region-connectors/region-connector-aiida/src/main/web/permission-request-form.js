import { html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/qr-code/qr-code.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/components/copy-button/copy-button.js";

const BASE_URL = new URL(import.meta.url).href.replace("ce.js", "");
const REQUEST_URL = BASE_URL + "permission-request";

class PermissionRequestForm extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedAttributes: { type: Object, attribute: "data-need-attributes" },
    _isConnected: { type: Boolean },
    _aiidaCode: { type: String },
  };

  qrCodeRef = createRef();

  constructor() {
    super();

    this._aiidaCode = null;
  }

  requestPermission(_event) {
    let body = {
      dataNeedId: this.dataNeedAttributes.id,
      connectionId: this.connectionId,
    };

    fetch(REQUEST_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((response) => (this._aiidaCode = response.json()))
      .then((json) => {
        this._aiidaCode = JSON.stringify(json);
        this.qrCodeRef.value.updateComplete.then(() => {
          this.qrCodeRef.value.style.visibility = "visible";
        });
      })
      .catch((error) => console.error(error));
  }

  convertToBase64(content) {
    // convert to UTF8 to ensure proper encoding of Unicode characters
    let bytes = new TextEncoder().encode(content);
    let binString = String.fromCodePoint(...bytes);
    return btoa(binString);
  }

  render() {
    return html`
      <p>
        Click the connect button below to generate a unique QR code that you can
        scan with the AIIDA app to set up data sharing with this service.
        <br />
        If you are not using the app, visit the Web-UI of your AIIDA instance to
        set up a new permission and enter the token below the QR code.
      </p>

      ${!this._aiidaCode
        ? html`<sl-button @click="${this.requestPermission}" variant="primary">
            Connect
          </sl-button>`
        : html`
            <sl-qr-code
              ${ref(this.qrCodeRef)}
              value="${this._aiidaCode}"
              radius="0.5"
              style="visibility: hidden"
              size="256"
            ></sl-qr-code>

            <br />
            <br />

            <sl-input
              label="AIIDA code"
              value="${this.convertToBase64(this._aiidaCode)}"
              help-text="Enter this code on the website of your AIIDA device"
              size="medium"
              readonly
            >
              <sl-copy-button
                value="${this.convertToBase64(this._aiidaCode)}"
                slot="suffix"
              ></sl-copy-button>
            </sl-input>
          `}
    `;
  }
}

export default PermissionRequestForm;
