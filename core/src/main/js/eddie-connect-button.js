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

setBasePath("https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn");

const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const COUNTRIES = new Set(PERMISSION_ADMINISTRATORS.map((pa) => pa.country));

const CORE_URL =
  import.meta.env.VITE_CORE_URL ??
  import.meta.url.replace("/lib/eddie-components.js", "");

const dialogOpenEvent = new Event("eddie-dialog-open", {
  bubbles: true,
  composed: true,
});

const dialogCloseEvent = new Event("eddie-dialog-close", {
  bubbles: true,
  composed: true,
});

function fetchJson(path) {
  return fetch(CORE_URL + path)
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

function getEnabledCountries(regionConnectors) {
  return regionConnectors
    .flatMap((rc) => rc.countryCodes)
    .map((countryCode) => countryCode.toLowerCase())
    .filter((country) => COUNTRIES.has(country));
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
    // Public properties
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

    // Event handlers
    onOpen: { type: String },
    onClose: { type: String },
    onStatusChange: { type: String },

    // Private properties
    _isValidConfiguration: { type: Boolean },
    _selectedCountry: { type: String },
    _selectedPermissionAdministrator: { type: Object },
    _filteredPermissionAdministrators: { type: Array },
  };

  static styles = css`
    :host {
      color: black;
      font-family:
        -apple-system,
        BlinkMacSystemFont,
        ‘Segoe UI’,
        Roboto,
        Helvetica,
        Arial,
        sans-serif,
        ‘Apple Color Emoji’,
        ‘Segoe UI Emoji’,
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

    .eddie-connect-button:disabled {
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
     * Undefined until the configuration has been validated.
     * If the configuration is valid, this will be set to true.
     * If the configuration is invalid, this will be set to false.
     * @type {boolean}
     * @private
     */
    this._isValidConfiguration = undefined;

    /**
     * Region connectors that are supported.
     * @type {string[]}
     * @private
     */
    this._enabledConnectors = [];

    /**
     * Region connectors that support the selected data need.
     * @type {string[]}
     * @private
     */
    this._supportedConnectors = [];

    /**
     * Permission administrators which region connector supports the selected data need.
     * @type {PermissionAdministrator[]}
     * @private
     */
    this._supportedPermissionAdministrators = [];

    /**
     * Country codes of all enabled region connectors.
     * @type {string[]}
     * @private
     */
    this._enabledCountries = [];

    /**
     * Country codes which region connectors support the selected data need.
     * @type {Set<string>}
     * @private
     */
    this._supportedCountries = undefined;

    /**
     * Permission administrators that match the selected country.
     * @type {PermissionAdministrator[]}
     * @private
     */
    this._filteredPermissionAdministrators = [];

    /**
     * Attributes of the chosen data need.
     * @type {DataNeedAttributes}
     * @private
     */
    this._dataNeedAttributes = undefined;

    /**
     * If data need is disabled.
     * @type {boolean}
     * @private
     */
    this._disabled = false;
  }

  connectedCallback() {
    super.connectedCallback();

    this.configure()
      .then(() => (this._isValidConfiguration = true))
      .catch(() => (this._isValidConfiguration = false));

    this.addRequestStatusHandlers();
  }

  openDialog() {
    this.dialogRef.value.show();
  }

  closeDialog() {
    this.dialogRef.value.hide();
  }

  reset() {
    this.replaceWith(this.cloneNode());
  }

  async getRegionConnectorElement() {
    const regionConnectorId =
      this._selectedPermissionAdministrator.regionConnector;
    console.log(regionConnectorId);

    const customElementName = regionConnectorId + "-pa-ce";

    if (!customElements.get(customElementName)) {
      // loaded module needs to have the custom element class as its default export
      try {
        const module = await import(
          /* @vite-ignore */
          `${CORE_URL}/region-connectors/${regionConnectorId}/ce.js`
        );
        customElements.define(customElementName, module.default);
      } catch (error) {
        // If multiple EDDIE button are preconfigured with the same region
        // connector, they may define its custom element at the same time.
        // This will cause an error, but it can be safely ignored.
        if (!customElements.get(customElementName)) {
          console.error(error);

          return html`<sl-alert variant="danger" open>
            <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
            Could not load region connector for
            ${this._selectedPermissionAdministrator.company}. Please contact the
            service provider.
          </sl-alert>`;
        }
      }
    }

    const element = document.createElement(customElementName);
    element.setAttribute("connection-id", this.connectionId);
    element.setAttribute("data-need-id", this.dataNeedId);
    element.setAttribute(
      "country-code",
      this._selectedPermissionAdministrator.country
    );
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
    return this._supportedPermissionAdministrators.find(
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

    if (!country) {
      this._filteredPermissionAdministrators = [];
      return;
    }

    // only show permission administrators that match the selected country
    this._filteredPermissionAdministrators =
      this._supportedPermissionAdministrators.filter(
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

    if (!this._dataNeedAttributes.enabled) {
      this._disabled = true;
      return;
    }

    this._enabledConnectors = await fetchJson(
      "/api/region-connectors-metadata"
    );

    this._enabledCountries = await getEnabledCountries(this._enabledConnectors);
    this._enabledCountries.sort((a, b) => a.localeCompare(b));

    this._supportedConnectors = await getSupportedRegionConnectors(
      this.dataNeedId
    );

    this._supportedPermissionAdministrators = PERMISSION_ADMINISTRATORS.filter(
      (pa) => this._supportedConnectors.includes(pa.regionConnector)
    );

    this._supportedCountries = new Set(
      this._supportedPermissionAdministrators.map((pa) => pa.country)
    );

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
    }

    if (this.isAiida()) {
      if (!this._supportedConnectors.includes("aiida")) {
        throw new Error(
          `AIIDA does not support the data need with id ${this.dataNeedId}.`
        );
      }

      this._presetPermissionAdministrator = {
        company: "AIIDA",
        regionConnector: "aiida",
      };
    }

    if (this._presetPermissionAdministrator) {
      this.selectPermissionAdministrator(this._presetPermissionAdministrator);
    }

    if (
      !this.permissionAdministratorId &&
      this.rememberPermissionAdministrator
    ) {
      this.loadPermissionAdministratorFromLocalStorage();
    }
  }

  isAiida() {
    return (
      this._dataNeedAttributes?.type === "genericAiida" ||
      this._dataNeedAttributes?.type === "smartMeterAiida"
    );
  }

  handleDialogShow(event) {
    // Only fire for the dialog itself, not for its children
    if (event.currentTarget !== event.target) {
      return;
    }

    this.dispatchEvent(dialogOpenEvent);
    Function(`"use strict";${this.onOpen}`)();
  }

  handleDialogHide(event) {
    // Only fire for the dialog itself, not for its children
    if (event.currentTarget !== event.target) {
      return;
    }

    this.dispatchEvent(dialogCloseEvent);
    Function(`"use strict";${this.onClose}`)();
  }

  addRequestStatusHandlers() {
    this.addEventListener("eddie-request-status", (event) => {
      const status = event.detail.status;

      // Execute the onStatusChange handler with the status as an argument
      Function(`"use strict";${this.onStatusChange}`)(status);

      // Execute the specific status handler if it exists
      const statusHandlerString = status.toLowerCase().replaceAll("_", "");
      const statusHandler = this.getAttribute(`on${statusHandlerString}`);
      if (statusHandler) {
        Function(`"use strict";${statusHandler}`)();
      }
    });

    // Handle creation event separately, as it is not passed as a status change
    this.addEventListener("eddie-request-created", (event) => {
      Function(`"use strict";${this.getAttribute("onCreated")}`)(event);
    });
  }

  simIsEnabled() {
    return this._enabledConnectors.some((rc) => rc.id === "sim");
  }

  render() {
    if (!this._isValidConfiguration) {
      return html`
        <button class="eddie-connect-button" disabled>
          ${unsafeSVG(buttonIcon)}
          <span>
            ${this._isValidConfiguration === undefined
              ? "Loading"
              : "Invalid configuration"}
          </span>
        </button>
      `;
    }

    if (this._disabled) {
      return html`
        <button class="eddie-connect-button" disabled>
          ${unsafeSVG(buttonIcon)}
          <span> Disabled Configuration </span>
        </button>
      `;
    }
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/themes/light.css"
      />
      <button class="eddie-connect-button" @click="${this.openDialog}">
        ${unsafeSVG(buttonIcon)}
        <span>Connect with EDDIE</span>
      </button>

      <sl-dialog
        ${ref(this.dialogRef)}
        style="--width: clamp(30rem, 100%, 45rem)"
        @sl-show="${this.handleDialogShow}"
        @sl-hide="${this.handleDialogHide}"
      >
        <div slot="label">${unsafeSVG(headerImage)}</div>

        <!-- Render data need summary -->
        <h2>Request for Permission</h2>
        <data-need-summary
          data-need-id="${this.dataNeedId}"
        ></data-need-summary>
        <br />

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
                help-text="${this._supportedCountries.size !==
                this._enabledCountries.length
                  ? "Some countries do not support the given data requirements."
                  : ""}"
              >
                ${this._enabledCountries.map(
                  (country) => html`
                    <sl-option
                      value="${country}"
                      ?disabled="${!this._supportedCountries.has(country)}"
                    >
                      ${COUNTRY_NAMES.of(country.toUpperCase())}
                    </sl-option>
                  `
                )}
                ${this.simIsEnabled()
                  ? html`
                      <sl-divider></sl-divider>
                      <small>Development</small>
                      <sl-option value="sim">Simulation</sl-option>
                    `
                  : ""}
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
                help-text="Typically your Distribution System Operator (DSO) or Transmission System Operator (TSO), depending on your national regulation."
                @sl-change="${this.handlePermissionAdministratorSelect}"
                value="${ifDefined(
                  this._selectedPermissionAdministrator?.companyId
                )}"
                ?disabled="${!this._selectedCountry ||
                !!this._presetPermissionAdministrator ||
                this._filteredPermissionAdministrators.length <= 1}"
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
