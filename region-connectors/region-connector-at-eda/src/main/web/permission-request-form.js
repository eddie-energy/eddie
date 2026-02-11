// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { html, nothing } from "lit";

import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/spinner/spinner.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/copy-button/copy-button.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    companyId: { attribute: "company-id" },
    companyName: { attribute: "company-name" },
    accountingPointId: { attribute: "accounting-point-id" },
    _isSubmitDisabled: { type: Boolean },
    _dataNeedIds: { type: Array },
    _createdCount: { type: Number },
    _sentCount: { type: Number },
    _cmRequestIds: { type: Array },
  };

  constructor() {
    super();
    this._isSubmitDisabled = false;
    this._createdCount = 0;
    this._sentCount = 0;
    this._cmRequestIds = [];
  }

  connectedCallback() {
    super.connectedCallback();
    this._dataNeedIds = this.dataNeedId.split(",");
    this.addEventListener("eddie-request-status", (event) => {
      const {
        additionalInformation: { cmRequestId },
        status,
      } = event.detail;

      if (status === "CREATED") {
        this._createdCount += 1;
      }

      if (status === "SENT_TO_PERMISSION_ADMINISTRATOR") {
        this._sentCount += 1;
      }

      if (cmRequestId && !this._cmRequestIds.includes(cmRequestId)) {
        this._cmRequestIds.push(cmRequestId);
      }
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    const jsonData = {
      meteringPointId: !!formData.get("meteringPointId")
        ? formData.get("meteringPointId")
        : null,
      dsoId: this.companyId,
      connectionId: this.connectionId,
      dataNeedIds: this._dataNeedIds,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(jsonData).catch((error) => {
      this._isSubmitDisabled = false;
      this.error(error);
    });
  }

  render() {
    return this._sentCount === 0
      ? html`
          <form id="request-form">
            <sl-input
              label="Zählpunktnummer"
              type="text"
              .helpText=${this.accountingPointId
                ? "The service has already provided a Zählpunktnummer. If this value is incorrect, please contact the service provider."
                : "Enter your 33-character Zählpunktnummer for the request to show up in your DSO portal. Leave blank to search for the generated Consent Request ID."}
              name="meteringPointId"
              minlength="33"
              maxlength="33"
              placeholder="${this.companyId}..."
              .value="${this.accountingPointId ?? nothing}"
              .disabled="${!!this.accountingPointId}"
            ></sl-input>

            <br />

            <div>
              <sl-button
                type="submit"
                variant="primary"
                ?disabled="${this._isSubmitDisabled}"
              >
                Connect
              </sl-button>
            </div>
          </form>

          ${this._createdCount > 0
            ? html`<br />
                <sl-alert open>
                  <sl-spinner slot="icon"></sl-spinner>

                  <p>
                    ${this._createdCount} of ${this._dataNeedIds.length}
                    permission requests were created successfully. Please wait
                    while we are sending the permission requests to the
                    permission administrator. This might take some time.
                  </p>
                </sl-alert>`
            : ""}
        `
      : html`
          <sl-alert open>
            <sl-icon slot="icon" name="info-circle"></sl-icon>

            <p>
              ${this._sentCount} of ${this._dataNeedIds.length} requests were
              successfully sent to the permission administrator. The Consent
              Request IDs for this connection are
              ${this._cmRequestIds.join(", ")}
            </p>

            <p>
              Further steps are required at the website of the permission
              administrator. Visit the website using the button below and look
              for your provided Zählpunktnummer or the Consent Request with one
              of the following IDs

            <ul>
              ${this._cmRequestIds.map(
                (cmRequestId) => html`
                  <li>
                    <span id="${cmRequestId}">${cmRequestId}</span>
                    <sl-copy-button from="${cmRequestId}"></sl-copy-button>
                  </li>
                `
              )}
            </ul>
            </p>

            ${
              this.jumpOffUrl
                ? html` <sl-button
                    href="${this.jumpOffUrl}"
                    target="_blank"
                    variant="primary"
                  >
                    Continue to ${this.companyName}
                  </sl-button>`
                : ""
            }
          </sl-alert>
        `;
  }
}

export default PermissionRequestForm;
