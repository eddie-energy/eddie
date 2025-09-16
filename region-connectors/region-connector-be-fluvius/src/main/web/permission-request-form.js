import { html } from "lit";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/radio-group/radio-group.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/components/radio/radio.js";
import 'https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/components/tooltip/tooltip.js';
import 'https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/components/icon/icon.js';

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    _requestStatus: { type: String },
    _shortUrlIdentifier: { type: String },
  };

  constructor() {
    super();
    this.addEventListener("eddie-request-status", (event) => {
      const {
        additionalInformation: { shortUrlIdentifier },
      } = event.detail;
      this._shortUrlIdentifier = shortUrlIdentifier;
    });
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    const payload = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
      flow: formData.get("flow"),
    };

    this.createPermissionRequest(payload).catch((error) => this.error(error));
  }

  render() {
    return html`
        <div>
            <h4>Select Account</h4>
            <form id="request-form">
                <sl-radio-group
                        label="Please select your account type:"
                        name="flow"
                        value="B2C"
                        help-text="Fluvius needs to know your account type in order to send the correct data."
                >
                    <sl-radio value="B2C">Personal Account</sl-radio>


                    <sl-radio value="B2B">Professional Account
                        <sl-tooltip content="For companies, local authorities, non-profit organizations etc.">
                            <sl-icon name="info-circle"></sl-icon>
                        </sl-tooltip>
                    </sl-radio>

                </sl-radio-group>

                <br/>

                <sl-button type="submit" variant="primary">Connect</sl-button>
            </form>
        </div>
        <div ?hidden="${!this._shortUrlIdentifier}">
            <p>Please accept the authorization request in your Fluvius portal.</p>
            <a href="${this.jumpOffUrl + this._shortUrlIdentifier}" target="_blank">
                Go to Fluvius
            </a>
        </div>
    `;
  }
}

export default PermissionRequestForm;
