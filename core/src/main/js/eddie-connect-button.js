import { css, html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";
import { until } from "lit/directives/until.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";
import { ifDefined } from "lit/directives/if-defined.js";

// Shoelace
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/dialog/dialog.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/icon/icon.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/select/select.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/option/option.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/divider/divider.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/spinner/spinner.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/tooltip/tooltip.js";

import { setBasePath } from "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/utilities/base-path.js";
import buttonIcon from "../resources/logo.svg?raw";
import headerImage from "../resources/header.svg?raw";

import PERMISSION_ADMINISTRATORS from "../../../../european-masterdata/src/main/resources/permission-administrators.json";
import { dataNeedSummary } from "./components/data-need-summary.js";

setBasePath("https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn");

const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const BASE_URL = import.meta.url.replace("/lib/eddie-components.js", "");

const dialogOpenEvent = new Event("eddie-dialog-open", {
  bubbles: true,
  composed: true,
});

const dialogCloseEvent = new Event("eddie-dialog-close", {
  bubbles: true,
  composed: true,
});

function fetchJson(path) {
  return fetch(BASE_URL + path)
    .then((response) => {
      if (!response.ok) {
        throw new Error(
          `Fetch to ${path} returned invalid status code ${response.status}`
        );
      }
      return response.json();
    })
    .catch((error) => console.error(error));
}

function getSupportedRegionConnectors(dataNeedId) {
  return fetchJson(`/api/region-connectors/data-needs/${dataNeedId}`).then(
    (json) => Object.keys(json).filter((key) => json[key].supportsDataNeed)
  );
}

/**
 *
 * @param {string} dataNeedId - The ID of the data need to fetch attributes for.
 * @returns {Promise<DataNeedAttributes | void>}
 */
function getDataNeedAttributes(dataNeedId) {
  return fetchJson(`/data-needs/api/${dataNeedId}`);
}

class EddieConnectButton extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id", type: String },
    dataNeedId: { attribute: "data-need-id", type: String },
    permissionAdministratorId: {
      attribute: "permission-administrator",
      type: String,
    },
    accountingPointId: { attribute: "accounting-point-id", type: String },
    rememberPermissionAdministrator: {
      attribute: "remember-permission-administrator",
      type: Object,
    },
    onOpen: { type: String },
    onClose: { type: String },

    _selectedCountry: { type: String },
    _selectedPermissionAdministrator: { type: Object },
    _availablePermissionAdministrators: { type: Array },
    _filteredPermissionAdministrators: { type: Array },
    _supportedConnectors: { type: Array },
    _availableCountries: { type: Array },
    _dataNeedAttributes: { type: Object },
    _isValidConfiguration: { type: Boolean, state: true },
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

    .eddie-connect-button--disabled {
      cursor: default;
      filter: grayscale(100%);
    }

    .version-indicator {
      font-size: var(--sl-font-size-x-small);
      color: var(--sl-color-neutral-500);
      padding: var(--sl-spacing-2x-small);
    }
  `;
  dialogRef = createRef();

  constructor() {
    super();

    /**
     * Region connectors that support the selected data need.
     * @type {string[]}
     * @private
     */
    this._supportedConnectors = [];

    this._availablePermissionAdministrators = [];
    this._availableCountries = [];

    /**
     * Permission Administrators that match the selected country.
     * @type {string[]}
     * @private
     */
    this._filteredPermissionAdministrators = [];

    /**
     * Attributes of the chosen data need.
     * @type {DataNeedAttributes}
     * @private
     */
    this._dataNeedAttributes = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._isValidConfiguration = this.configure()
      .then(() => true)
      .catch((error) => {
        console.error(error);
        return false;
      });
  }

  async connect() {
    this.dialogRef.value.show();

    if (
      !this.permissionAdministratorId &&
      this.rememberPermissionAdministrator
    ) {
      this.loadPermissionAdministratorFromLocalStorage();
    }
  }

  async getRegionConnectorElement() {
    const regionConnectorId =
      this._selectedPermissionAdministrator.regionConnector;

    const customElementName = regionConnectorId + "-pa-ce";

    if (!customElements.get(customElementName)) {
      // loaded module needs to have the custom element class as its default export
      try {
        const module = await import(
          `${BASE_URL}/region-connectors/${regionConnectorId}/ce.js`
        );
        customElements.define(customElementName, module.default);
      } catch (error) {
        console.error(error);

        return html`<sl-alert variant="danger" open>
          <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
          Could not load region connector for
          ${this._selectedPermissionAdministrator.company}. Please contact the
          service provider.
        </sl-alert>`;
      }
    }

    const element = document.createElement(customElementName);
    element.setAttribute("connection-id", this.connectionId);
    element.setAttribute("data-need-id", this.dataNeedId);
    element.setAttribute(
      "jump-off-url",
      this._selectedPermissionAdministrator.jumpOffUrl
    );
    element.setAttribute(
      "company-id",
      this._selectedPermissionAdministrator.companyId
    );

    if (this.accountingPointId) {
      element.setAttribute("accounting-point-id", this.accountingPointId);
    }

    const notificationHandler = document.createElement(
      "eddie-notification-handler"
    );
    const requestStatusHandler = document.createElement(
      "eddie-request-status-handler"
    );

    requestStatusHandler.appendChild(element);
    notificationHandler.appendChild(requestStatusHandler);

    return notificationHandler;
  }

  /**
   *
   * @param {string} companyId The company ID of the permission administrator.
   * @returns {PermissionAdministrator | undefined} The first permission administrator with the given company ID or undefined if none is found.
   */
  getPermissionAdministratorByCompanyId(companyId) {
    return this._availablePermissionAdministrators.find(
      (pa) => pa.companyId === companyId
    );
  }

  handlePermissionAdministratorSelect(event) {
    const companyId = event.target.value;
    this._selectedPermissionAdministrator =
      this.getPermissionAdministratorByCompanyId(companyId);

    if (this.rememberPermissionAdministrator) {
      localStorage.setItem("permissionAdministrator", companyId);
    }
  }

  selectPermissionAdministrator(permissionAdministrator) {
    this._selectedPermissionAdministrator = permissionAdministrator;
    this.selectCountry(permissionAdministrator?.country);
  }

  loadPermissionAdministratorFromLocalStorage() {
    const id = localStorage.getItem("permissionAdministrator");

    if (id) {
      const pa = this.getPermissionAdministratorByCompanyId(id);

      if (pa) {
        this.selectPermissionAdministrator(pa);
      }
    }
  }

  handleCountrySelect(event) {
    this._selectedPermissionAdministrator = null;

    if (event.target.value === "sim") {
      this._selectedCountry = null;
      this._selectedPermissionAdministrator = { regionConnector: "sim" };
    } else {
      this.selectCountry(event.target.value);
    }
  }

  selectCountry(country) {
    this._selectedCountry = country;

    // only show permission administrators that match the selected country
    this._filteredPermissionAdministrators =
      this._availablePermissionAdministrators.filter(
        (pa) => pa.country === this._selectedCountry
      );

    // automatically select the only available permission administrator
    if (this._filteredPermissionAdministrators.length === 1) {
      this._selectedPermissionAdministrator =
        this._filteredPermissionAdministrators[0];
    }
  }

  async configure() {
    if (!this.dataNeedId) {
      throw new Error("EDDIE button loaded without data-need-id.");
    }

    if (this.accountingPointId && !this.permissionAdministratorId) {
      throw new Error(
        "Accounting point specified without permission administrator."
      );
    }

    this._dataNeedAttributes = await getDataNeedAttributes(this.dataNeedId);

    if (!this._dataNeedAttributes) {
      throw new Error(`Invalid Data Need ${this.dataNeedId}`);
    }

    this._supportedConnectors = await getSupportedRegionConnectors(
      this.dataNeedId
    );

    this._availablePermissionAdministrators = PERMISSION_ADMINISTRATORS.filter(
      (pa) => this._supportedConnectors.includes(pa.regionConnector)
    );

    this._availableCountries = [
      ...new Set(
        this._availablePermissionAdministrators.map((pa) => pa.country)
      ),
    ];

    if (this.permissionAdministratorId) {
      const pa = this.getPermissionAdministratorByCompanyId(
        this.permissionAdministratorId
      );

      if (!pa) {
        throw new Error(
          `Permission Administrator ${this.permissionAdministratorId} does not support the data need with id ${this.dataNeedId}.`
        );
      }

      this._presetPermissionAdministrator = pa;
      this.selectPermissionAdministrator(pa);
    }

    if (this.isAiida()) {
      if (!this._supportedConnectors.includes("aiida")) {
        throw new Error(
          `AIIDA does not support the data need with id ${this.dataNeedId}.`
        );
      }

      this.selectAiida();
    } else {
      this.selectPermissionAdministrator(this._presetPermissionAdministrator);
    }
  }

  isAiida() {
    return (
      this._dataNeedAttributes?.type === "genericAiida" ||
      this._dataNeedAttributes?.type === "smartMeterAiida"
    );
  }

  selectAiida() {
    this._selectedCountry = null;
    this._selectedPermissionAdministrator = { regionConnector: "aiida" };
  }

  handleDialogShow() {
    this.dispatchEvent(dialogOpenEvent);
    Function(`"use strict";${this.onOpen}`)();
  }

  handleDialogHide() {
    this.dispatchEvent(dialogCloseEvent);
    Function(`"use strict";${this.onClose}`)();
  }

  render() {
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn/themes/light.css"
      />
      ${until(
        this._isValidConfiguration.then((isValid) =>
          isValid
            ? html`
                <button class="eddie-connect-button" @click="${this.connect}">
                  ${unsafeSVG(buttonIcon)}
                  <span>Connect with EDDIE</span>
                </button>
              `
            : html`
                <button
                  class="eddie-connect-button eddie-connect-button--disabled"
                  disabled
                >
                  ${unsafeSVG(buttonIcon)}
                  <span>Invalid configuration</span>
                </button>
              `
        )
      )}

      <sl-dialog
        ${ref(this.dialogRef)}
        style="--width: clamp(30rem, 100%, 45rem)"
        @sl-show="${this.handleDialogShow}"
        @sl-hide="${this.handleDialogHide}"
      >
        <div slot="label">${unsafeSVG(headerImage)}</div>

        <!-- Render data need summary -->
        ${this._dataNeedAttributes
          ? dataNeedSummary(this._dataNeedAttributes)
          : ""}

        <!-- Render data need summary -->
        ${this._dataNeedAttributes
          ? dataNeedSummary(this._dataNeedAttributes)
          : ""}

        <!-- Render country selection -->
        ${!this.isAiida()
          ? html`
              <sl-select
                label="Country"
                placeholder="Select your country"
                @sl-change="${this.handleCountrySelect}"
                value="${this._selectedPermissionAdministrator?.country ??
                this._selectedCountry ??
                ""}"
                ?disabled="${!!this._presetPermissionAdministrator}"
              >
                ${this._availableCountries.map(
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
            `
          : ""}

        <!-- Render permission administrator selection -->
        ${this._selectedCountry
          ? html`
              <sl-select
                label="Permission Administrator"
                placeholder="Select your Permission Administrator"
                help-text="Typically your Distribution System Operator (DSO) or Metering Point Administrator (MDA), depending on your national regulation."
                @sl-change="${this.handlePermissionAdministratorSelect}"
                value="${ifDefined(
                  this._selectedPermissionAdministrator?.companyId
                )}"
                ?disabled="${!!this._presetPermissionAdministrator ||
                this._filteredPermissionAdministrators.length === 1}"
              >
                ${this._filteredPermissionAdministrators.map(
                  (pa) => html`
                    <sl-option value="${pa.companyId}">
                      ${pa.company}
                    </sl-option>
                  `
                )}
              </sl-select>
              <br />
            `
          : ""}

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

        <div slot="footer">
          <div class="version-indicator">
            <i>EDDIE Version: __EDDIE_VERSION__</i>
          </div>
        </div>
      </sl-dialog>
    `;
  }
}

export default EddieConnectButton;
