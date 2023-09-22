import { css, html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";
import { until } from "lit/directives/until.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";

// Shoelace
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/dialog/dialog.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/icon/icon.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/select/select.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/option/option.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/divider/divider.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/spinner/spinner.js";

// Only used for DataNeed modification
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/input/input.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/details/details.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/checkbox/checkbox.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/components/button/button.js";

import { setBasePath } from "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/utilities/base-path.js";
import buttonIcon from "../resources/logo.svg?raw";
import headerImage from "../resources/header.svg?raw";

import PERMISSION_ADMINISTRATORS from "../resources/permission-administrators.json";

setBasePath("https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn");

const COUNTRIES = [
  ...new Set(PERMISSION_ADMINISTRATORS.map((item) => item.country)),
];
const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const BASE_URL = new URL(import.meta.url).origin;
const METADATA_URL = new URL("/api/region-connectors-metadata", BASE_URL);

function getRegionConnectors() {
  return fetch(METADATA_URL)
    .then((response) => response.json())
    .then((json) => Object.fromEntries(json.map((it) => [it.mdaCode, it])));
}

function getDataNeedAttributes(dataNeedId) {
  return fetch(new URL(`/api/data-needs/${dataNeedId}`, BASE_URL))
    .then((response) => response.json())
    .catch((err) => console.error(err));
}

class EddieConnectButton extends LitElement {
  static properties = {
    connectionId: { attribute: "connectionid" },
    dataNeedId: { attribute: "data-need-id" },
    _selectedCountry: { type: String },
    _selectedPermissionAdministrator: { type: Object },
    _availableConnectors: { type: Object },
    _dataNeedAttributes: { type: Object },
    _allowDataNeedModifications: { type: Boolean },
  };

  static CONTEXT_PATH = new URL(import.meta.url).pathname.replace(
    /\/lib\/.*$/,
    ""
  );

  dialogRef = createRef();
  permissionAdministratorSelectRef = createRef();

  static styles = css`
    .eddie-connect-button {
      display: flex;
      align-items: center;
      gap: 1rem;
      background: white;
      border: 2px solid #017aa0;
      color: #017aa0;
      border-radius: 9999px;
      padding: 0.5rem 1.25rem 0.5rem 1rem;
      font-weight: bold;
      cursor: pointer;
    }
  `;

  constructor() {
    super();
    this._availableConnectors = {};
    this._dataNeedAttributes = {};
    this._allowDataNeedModifications = true;
  }

  async connect() {
    this._availableConnectors = await getRegionConnectors();
    this._dataNeedAttributes = await getDataNeedAttributes(this.dataNeedId);

    this.dialogRef.value.show();
  }

  closePopup() {
    this.dialogRef.value.hide();
  }

  async getRegionConnectorElement() {
    const regionConnectorId =
      this._selectedPermissionAdministrator.regionConnector;
    const customElementName = regionConnectorId + "-pa-ce";

    if (!customElements.get(customElementName)) {
      const regionConnector = this._availableConnectors[regionConnectorId];
      // loaded module needs to have the custom element class as it's default export
      const module = await import(`${BASE_URL}${regionConnector.urlPath}ce.js`);
      customElements.define(customElementName, module.default);
    }

    const element = document.createElement(customElementName);
    element.setAttribute("connection-id", this.connectionId);
    element.setAttribute(
      "allow-data-need-modifications",
      this._allowDataNeedModifications
    );
    element.setAttribute(
      "data-need-attributes",
      JSON.stringify(this._dataNeedAttributes)
    );
    element.setAttribute(
      "jump-off-url",
      this._selectedPermissionAdministrator.jumpOffUrl
    );

    return element;
  }

  handleCountrySelect(event) {
    this._selectedPermissionAdministrator = null;

    // clear permission administrator value on country change
    if (this.permissionAdministratorSelectRef.value) {
      this.permissionAdministratorSelectRef.value.value = "";
    }

    if (event.target.value === "sim") {
      this._selectedCountry = null;
      this._selectedPermissionAdministrator = { regionConnector: "sim" };
    } else {
      this._selectedCountry = event.target.value;
    }
  }

  handlePermissionAdministratorSelect(event) {
    this._selectedPermissionAdministrator =
      PERMISSION_ADMINISTRATORS[event.target.value];
  }

  handleDataNeedModifications(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    this._dataNeedAttributes.durationStart = formData.get("durationStart");
    this._dataNeedAttributes.durationOpenEnd = formData.get("durationOpenEnd");
    this._dataNeedAttributes.durationEnd = formData.get("durationEnd");
  }

  render() {
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.8.0/cdn/themes/light.css"
      />

      <button class="eddie-connect-button" @click="${this.connect}">
        ${unsafeSVG(buttonIcon)}
        <span>Connect with EDDIE</span>
      </button>

      <sl-dialog
        ${ref(this.dialogRef)}
        style="--width: clamp(30rem, 100%, 45rem)"
      >
        <div slot="label">${unsafeSVG(headerImage)}</div>

        <sl-alert open>
          <sl-icon slot="icon" name="info-circle"></sl-icon>
          This service is requesting: ${this._dataNeedAttributes.description}
        </sl-alert>

        <br />

        ${this._allowDataNeedModifications
          ? html`
              <sl-details
                summary="The service allows the modification of data needs."
              >
                <form @submit="${this.handleDataNeedModifications}">
                  <sl-input
                    label="Connection ID"
                    name="connectionId"
                    .value="${this.connectionId}"
                    disabled
                    readonly
                  ></sl-input
                  ><br />
                  <sl-input
                    label="DataNeed ID"
                    name="id"
                    .value="${this._dataNeedAttributes.id}"
                    disabled
                    readonly
                  ></sl-input
                  ><br />
                  <sl-input
                    label="Granularity"
                    name="granularity"
                    .value="${this._dataNeedAttributes.granularity}"
                    disabled
                    readonly
                  ></sl-input
                  ><br />
                  <sl-input
                    label="Duration Start"
                    name="durationStart"
                    .value="${this._dataNeedAttributes.durationStart}"
                  ></sl-input
                  ><br />
                  <div>
                    <sl-checkbox
                      name="durationOpenEnd"
                      .checked="${this._dataNeedAttributes.durationOpenEnd}"
                      >Duration Open End</sl-checkbox
                    >
                  </div>
                  <br />
                  <sl-input
                    label="Duration End"
                    .value="${this._dataNeedAttributes.durationEnd}"
                  ></sl-input>
                  <br />
                  <sl-button type="submit">Save</sl-button>
                </form>
              </sl-details>

              <br />
            `
          : ""}

        <sl-select
          label="Country"
          placeholder="Select your country"
          @sl-change="${this.handleCountrySelect}"
        >
          ${COUNTRIES.map(
            (country) => html`
              <sl-option value="${country}">
                ${COUNTRY_NAMES.of(country.toUpperCase())}
              </sl-option>
            `
          )}
          <sl-divider></sl-divider>
          <small>Development</small>
          <sl-option value="sim">Simulation</sl-option>
        </sl-select>

        <br />

        ${this._selectedCountry
          ? html`
              <sl-select
                label="Permission Administrator"
                placeholder="Select your Permission Administrator"
                @sl-change="${this.handlePermissionAdministratorSelect}"
                ${ref(this.permissionAdministratorSelectRef)}
              >
                ${PERMISSION_ADMINISTRATORS.filter(
                  (pa) => pa.country === this._selectedCountry
                ).map(
                  (pa) => html`
                    <sl-option
                      value="${PERMISSION_ADMINISTRATORS.findIndex(
                        (item) => item === pa
                      )}"
                      .disabled="${!this._availableConnectors[
                        pa.regionConnector
                      ]}"
                    >
                      ${pa.company}
                    </sl-option>
                  `
                )}
              </sl-select>
            `
          : ""}

        <br />

        <div>
          ${this._selectedPermissionAdministrator
            ? html`
                ${until(
                  this.getRegionConnectorElement(),
                  html`<sl-spinner></sl-spinner>`
                )}
              `
            : html`
                <sl-alert variant="warning" open>
                  <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
                  Please select your country and permission administrator to
                  continue.
                </sl-alert>
              `}
        </div>

        <br />
      </sl-dialog>
    `;
  }
}

export default EddieConnectButton;
