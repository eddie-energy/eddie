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

const BASE_URL = import.meta.url.replace("/lib/eddie-components.js", "");

function fetchJson(path) {
  return fetch(BASE_URL + path)
    .then((response) => response.json())
    .catch((error) => console.error(error));
}

function getRegionConnectors() {
  return fetchJson("/api/region-connectors-metadata").then((json) =>
    Object.fromEntries(json.map((it) => [it.id, it]))
  );
}

function getDataNeedAttributes(dataNeedId) {
  return fetchJson(`/api/data-needs/${dataNeedId}`);
}

function shortISOString(date) {
  return date.toISOString().substring(0, 10);
}

function dateFromDuration(duration) {
  const date = new Date();
  date.setDate(date.getDate() + duration);
  return shortISOString(date);
}

function durationFromDateString(dateString) {
  const now = new Date();
  const date = new Date(dateString);

  return Math.ceil((date - now) / (1000 * 60 * 60 * 24));
}

class EddieConnectButton extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    allowDataNeedModifications: { attribute: "allow-data-need-modifications" },
    allowDataNeedSelection: { attribute: "allow-data-need-selection" },

    _dataNeedIds: { type: Array },
    _selectedCountry: { type: String },
    _selectedPermissionAdministrator: {
      type: Object,
      name: "selected-permission-administrator",
    },
    _availableConnectors: { type: Object },
    _dataNeedAttributes: { type: Object },
    _dataNeedTypes: { type: Array },
    _dataNeedGranularities: { type: Array },
  };
  static styles = css`
    :host {
      color: black;
      font-family: -apple-system, BlinkMacSystemFont, ‘Segoe UI’, Roboto,
        Helvetica, Arial, sans-serif, ‘Apple Color Emoji’, ‘Segoe UI Emoji’,
        ‘Segoe UI Symbol’;
      font-size: 16px;
      font-weight: normal;
    }

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
  dialogRef = createRef();
  permissionAdministratorSelectRef = createRef();

  constructor() {
    super();
    this._availableConnectors = {};
    this._dataNeedAttributes = {};
    this._dataNeedIds = [];
    this._dataNeedTypes = [];
    this._dataNeedGranularities = [];
  }

  async connect() {
    if (!this.dataNeedId && !this.allowDataNeedSelection) {
      console.error(
        "EDDIE button loaded without data-need-id or allow-data-need-selection."
      );
    }

    if (this.allowDataNeedSelection) {
      this._dataNeedIds = await fetchJson("/api/data-needs");
    }

    if (this.dataNeedId) {
      this._dataNeedAttributes = await getDataNeedAttributes(this.dataNeedId);
    }

    if (this.allowDataNeedModifications) {
      this._dataNeedTypes = await fetchJson("/api/data-needs/types");
      this._dataNeedGranularities = await fetchJson(
        "/api/data-needs/granularities"
      );
    }

    this._availableConnectors = await getRegionConnectors();

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
      // loaded module needs to have the custom element class as its default export
      const module = await import(
        `${BASE_URL}/region-connectors/${regionConnector.id}/ce.js`
      );
      customElements.define(customElementName, module.default);
    }

    const element = document.createElement(customElementName);
    element.setAttribute("connection-id", this.connectionId);
    element.setAttribute(
      "allow-data-need-modifications",
      this.allowDataNeedModifications
    );
    element.setAttribute(
      "data-need-attributes",
      JSON.stringify(this._dataNeedAttributes)
    );
    element.setAttribute(
      "jump-off-url",
      this._selectedPermissionAdministrator.jumpOffUrl
    );
    element.setAttribute(
      "company-id",
      this._selectedPermissionAdministrator.companyId
    );

    return element;
  }

  async handleDataNeedSelect(event) {
    this._selectedPermissionAdministrator = null;

    this.dataNeedId = event.target.value;
    this._dataNeedAttributes = await getDataNeedAttributes(this.dataNeedId);
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

  handleDataNeedModification(event) {
    event.preventDefault();

    const formData = new FormData(event.target);

    this._dataNeedAttributes.type = formData.get("type");
    this._dataNeedAttributes.granularity = formData.get("granularity");
    this._dataNeedAttributes.durationStart = durationFromDateString(
      formData.get("startDate")
    );
    this._dataNeedAttributes.durationOpenEnd =
      formData.get("durationOpenEnd") === "on";
    this._dataNeedAttributes.durationEnd = durationFromDateString(
      formData.get("endDate")
    );

    this.requestUpdate(
      "selected-permission-administrator",
      this._selectedPermissionAdministrator
    );
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

        ${this.allowDataNeedSelection
          ? html`
              <sl-select
                label="Data need specification"
                placeholder="Select a data need"
                @sl-change="${this.handleDataNeedSelect}"
                help-text="The service allows the selection of a data need. This feature is meant for development purposes only."
              >
                ${this._dataNeedIds.map(
                  (id) => html` <sl-option value="${id}">${id}</sl-option> `
                )}
              </sl-select>
              <br />
            `
          : ""}
        ${this._dataNeedAttributes.description
          ? html`
              <sl-alert open>
                <sl-icon slot="icon" name="info-circle"></sl-icon>
                This service is requesting:
                ${this._dataNeedAttributes.description}
              </sl-alert>
              <br />
            `
          : ""}
        ${this.allowDataNeedModifications && this._dataNeedAttributes.id
          ? html`
              <sl-details
                summary="The service allows the modification of data needs. This feature is meant for development purposes only."
              >
                <form @submit="${this.handleDataNeedModification}">
                  <sl-input
                    label="Connection ID"
                    name="connectionId"
                    value="${this.connectionId}"
                    disabled
                    readonly
                  ></sl-input>
                  <br />
                  <sl-input
                    label="DataNeed ID"
                    name="id"
                    value="${this._dataNeedAttributes.id}"
                    disabled
                    readonly
                  ></sl-input>
                  <br />
                  <sl-select
                    label="DataNeed Type"
                    name="type"
                    value="${this._dataNeedAttributes.type}"
                  >
                    ${this._dataNeedTypes.map(
                      (value) =>
                        html` <sl-option value="${value}">${value}</sl-option> `
                    )}
                  </sl-select>
                  <br />
                  <sl-select
                    label="Granularity"
                    name="granularity"
                    value="${this._dataNeedAttributes.granularity}"
                  >
                    ${this._dataNeedGranularities.map(
                      (value) =>
                        html` <sl-option value="${value}">${value}</sl-option> `
                    )}
                  </sl-select>
                  <br />
                  <sl-input
                    label="Start Date"
                    name="startDate"
                    type="date"
                    value="${dateFromDuration(
                      this._dataNeedAttributes.durationStart
                    )}"
                  ></sl-input>
                  <br />
                  <div>
                    <sl-checkbox
                      name="durationOpenEnd"
                      .checked="${this._dataNeedAttributes.durationOpenEnd}"
                    >
                      Open End
                    </sl-checkbox>
                  </div>
                  <br />
                  <sl-input
                    label="End Date"
                    name="endDate"
                    type="date"
                    value="${dateFromDuration(
                      this._dataNeedAttributes.durationEnd
                    )}"
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
                  html` <sl-spinner></sl-spinner>`
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