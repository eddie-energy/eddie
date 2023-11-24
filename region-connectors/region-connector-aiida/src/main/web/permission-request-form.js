import { html, LitElement } from "lit";
import { createRef } from "lit/directives/ref.js";

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

  tooltipRef = createRef();

  constructor() {
    super();

    this._aiidaCode = null;
  }

  requestPermission(event) {
    let body = {
      dataNeedId: this.dataNeedAttributes.id,
      connectionId: this.connectionId,
    };

    // CORS doesn't succeed --> use direct URL instead of proxy until Spring migration is done
    let tempUrl =
      "http://localhost:9988/region-connectors/aiida/permission-request";
    fetch(tempUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((response) => (this._aiidaCode = response.json()))
      .then((json) => {
        this._aiidaCode = JSON.stringify(json);
      })
      .catch((error) => console.error(error));
  }

  render() {
    return html`
      <p>
        Here should be instructions on how to use this form to connect with
        AIIDA.
      </p>

      ${!this._aiidaCode
        ? html`<sl-button @click="${this.requestPermission}" variant="primary">
            Connect
          </sl-button>`
        : html`
            <sl-qr-code
              value="${this._aiidaCode}"
              radius="0.5"
              size="256"
            ></sl-qr-code>

            <br />
            <br />

            <sl-input
              label="AIIDA code"
              value="${window.btoa(this._aiidaCode)}"
              help-text="Enter this code on the website of your AIIDA device"
              size="medium"
              readonly
            >
              <sl-copy-button
                value="${window.btoa(this._aiidaCode)}"
                slot="suffix"
              ></sl-copy-button>
            </sl-input>
          `}
    `;
  }
}

export default PermissionRequestForm;
