// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/qr-code/qr-code.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/copy-button/copy-button.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    _isConnected: { type: Boolean },
    _aiidaCode: { type: String },
    _isSubmitDisabled: { type: Boolean },
  };

  constructor() {
    super();

    this._aiidaCode = "";
    this._isSubmitDisabled = false;
  }

  requestPermission(_event) {
    let body = {
      dataNeedId: this.dataNeedId,
      connectionId: this.connectionId,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(body)
      .then((result) => {
        this._aiidaCode = JSON.stringify(result);
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        this.error(error);
      });
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
        ? html`<sl-button
            @click="${this.requestPermission}"
            variant="primary"
            ?disabled="${this._isSubmitDisabled}"
          >
            Connect
          </sl-button>`
        : html`
            <sl-qr-code
              value="${this.convertToBase64(this._aiidaCode)}"
              label="AIIDA QR code"
              radius="0.5"
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
