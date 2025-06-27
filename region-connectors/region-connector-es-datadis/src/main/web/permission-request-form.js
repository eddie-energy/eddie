import { html, nothing } from "lit";
import { ifDefined } from "lit/directives/if-defined.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";
import PermissionRequestFormBase from "../../../../shared/src/main/web/permission-request-form-base.js";
import logo from "../resources/datadis-logo.svg?raw";

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/spinner/spinner.js";

class PermissionRequestForm extends PermissionRequestFormBase {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    accountingPointId: { attribute: "accounting-point-id" },
    jumpOffUrl: { attribute: "jump-off-url" },
    _isSentToPermissionAdministrator: { type: Boolean },
    _isSubmitDisabled: { type: Boolean },
    _isVerifying: { type: Boolean },
  };

  permissionId = null;
  accessToken = null;

  constructor() {
    super();
  }

  connectedCallback() {
    super.connectedCallback();

    this.addEventListener(
      "eddie-request-sent-to-permission-administrator",
      () => {
        this._isSentToPermissionAdministrator = true;
      }
    );
  }

  handleSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    let payload = {
      connectionId: this.connectionId,
      meteringPointId: formData.get("meteringPointId"),
      nif: formData.get("nif"),
      dataNeedId: this.dataNeedId,
    };

    this._isSubmitDisabled = true;

    this.createPermissionRequest(payload)
      .then(({ permissionId, accessToken }) => {
        this.permissionId = permissionId;
        this.accessToken = accessToken;
      })
      .catch((error) => {
        this._isSubmitDisabled = false;
        this.error(error);
      });
  }

  validate() {
    const nifInput = this.renderRoot.querySelector("#nif");
    const nif = nifInput.value;
    if (this.validateIdentifier(nif)) {
      nifInput.setCustomValidity("");
    } else {
      nifInput.setCustomValidity("Invalid NIF");
    }
  }

  /**
   * Checks if identifier is a valid NIF or CIF
   * @param {String} nif a NIF or CIF
   */
  validateIdentifier(nif) {
    nif = nif.replaceAll(" ", "").toUpperCase();
    return this.isNif(nif) || this.isCif(nif);
  }

  /**
   * See https://es.wikipedia.org/wiki/N%C3%BAmero_de_identificaci%C3%B3n_fiscal#NIF_de_personas_f%C3%ADsicas
   * @param {String} nif
   * @return {Boolean} if valid Nif
   */
  isNif(nif) {
    const nifLetters = "TRWAGMYFPDXBNJZSQVHLCKE";
    const dniRegex = /^(\d{8})([A-HJ-NP-TV-Z])$/;
    const dniRes = dniRegex.exec(nif);
    if (dniRes) {
      return nifLetters[parseInt(dniRes[1]) % 23] === dniRes[2];
    }
    const klmNif = /^[KLM](\d{7})([A-HJ-NP-TV-Z])$/;
    const klmNifRes = klmNif.exec(nif);
    if (klmNifRes) {
      return nifLetters[parseInt(klmNifRes[1]) % 23] === klmNifRes[2];
    }
    const nieRegex = /^([XYZ])(\d{7})([A-HJ-NP-TV-Z])$/;
    const nieRes = nieRegex.exec(nif);
    if (nieRes) {
      const prefix = nieRes[1].charCodeAt(0) - "X".charCodeAt(0);
      const securityLetter = parseInt(`${prefix}${nieRes[2]}`) % 23;
      return nifLetters[securityLetter] === nieRes[3];
    }
    return false;
  }

  /**
   * See https://es.wikipedia.org/wiki/C%C3%B3digo_de_identificaci%C3%B3n_fiscal
   * @param {String} cif
   * @return {Boolean} if valid CIF
   */
  isCif(cif) {
    const cifLetters = "JABCDEFGHI";
    const numberType = "ABCDEFGHJUV";
    const cifRegex = /^([ABCDEFGHJNPQRSUVW])(\d{7})(\d)$/;
    const cifRes = cifRegex.exec(cif);
    if (!cifRes) {
      return false;
    }
    const type = cifRes[1];
    const numbers = cifRes[2];
    const securityValue = cifRes[3];
    let evenSum = 0;
    let oddSum = 0;
    for (let i = 0; i < numbers.length; i++) {
      const num = parseInt(numbers.charAt(i));
      if ((i + 1) % 2 === 0) {
        evenSum += num;
      } else {
        oddSum += (num * 2)
          .toString()
          .split("")
          .reduce((sum, digit) => sum + parseInt(digit), 0);
      }
    }
    const sum = evenSum + oddSum;
    const sumStr = sum.toString();
    const check = 10 - parseInt(sumStr.charAt(sumStr.length - 1));
    if (numberType.includes(type)) {
      return parseInt(securityValue) === check;
    } else {
      return securityValue === cifLetters.charAt(check);
    }
  }

  accepted() {
    fetch(`${this.requestUrl}/${this.permissionId}/accepted`, {
      method: "PATCH",
      headers: {
        Authorization: "Bearer " + this.accessToken,
      },
    })
      .then(() => {
        this._isVerifying = true;
      })
      .catch((error) => {
        this._isVerifying = false;
        this.error(error);
      });
  }

  rejected() {
    fetch(`${this.requestUrl}/${this.permissionId}/rejected`, {
      method: "PATCH",
      headers: {
        Authorization: "Bearer " + this.accessToken,
      },
    }).catch((error) => {
      this.error(error);
    });
  }

  render() {
    return html`
      <form
        id="request-form"
        ?hidden="${this._isSentToPermissionAdministrator}"
      >
        <sl-input
          label="CUPS"
          id="meteringPointId"
          type="text"
          name="meteringPointId"
          .helpText=${this.accountingPointId
            ? "The service has already provided a CUPS value. If this value is incorrect, please contact the service provider."
            : nothing}
          .value="${ifDefined(this.accountingPointId)}"
          .disabled="${!!this.accountingPointId}"
          required
        ></sl-input>

        <br />

        <sl-input
          label="DNI/NIF"
          type="text"
          id="nif"
          name="nif"
          placeholder="25744101M"
          help-text="We require the identification number you use to log into the Datadis web portal to request permission."
          required
          @sl-change="${this.validate}"
        ></sl-input>

        <br />

        <sl-button
          ?disabled="${this._isSubmitDisabled}"
          type="submit"
          variant="primary"
        >
          Connect
        </sl-button>
      </form>

      <div ?hidden="${!this._isSentToPermissionAdministrator}">
        <p>
          A request for permission has been sent to Datadis. Please accept the
          authorization request in your Datadis portal.
        </p>
        <a
          href="${this.jumpOffUrl}"
          target="_blank"
          style="display: inline-block; background: #5D208B; border-radius: 2em; padding: 0.5em 1em 0.5em 1em"
        >
          ${unsafeSVG(logo)}
        </a>
        <p>
          Please let us know once you have accepted or rejected the
          authorization request.
        </p>
        <div>
          <sl-button
            variant="success"
            @click="${this.accepted}"
            ?disabled="${this._isVerifying}"
          >
            Accepted
          </sl-button>
          <sl-button
            variant="danger"
            @click="${this.rejected}"
            ?disabled="${this._isVerifying}"
          >
            Rejected
          </sl-button>
        </div>

        ${this._isVerifying
          ? html`
              <br />
              <sl-alert open>
                <sl-spinner slot="icon"></sl-spinner>

                <p>
                  We are verifying your response with Datadis. This might take a
                  few minutes.
                </p>
              </sl-alert>
            `
          : nothing}
      </div>
    `;
  }
}

export default PermissionRequestForm;
