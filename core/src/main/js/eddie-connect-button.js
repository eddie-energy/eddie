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
import { DATA_NEED_TOOLTIPS } from "./constants/data-need-tooltips.js";
import { GRANULARITIES } from "./constants/granularities.js";

import { relativeDateFromDuration } from "./duration.js";

setBasePath("https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.11.2/cdn");

const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const BASE_URL = import.meta.url.replace("/lib/eddie-components.js", "");

function fetchJson(path) {
  return fetch(BASE_URL + path)
    .then((response) => response.ok && response.json())
    .catch((error) => console.error(error));
}

function getRegionConnectors() {
  return fetchJson("/api/region-connectors-metadata").then((json) =>
    Object.fromEntries(json.map((it) => [it.id, it]))
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
    allowDataNeedSelection: {
      attribute: "allow-data-need-selection",
      type: Object,
    },
    permissionAdministratorId: {
      attribute: "permission-administrator",
      type: String,
    },
    accountingPointId: { attribute: "accounting-point-id", type: String },
    rememberPermissionAdministrator: {
      attribute: "remember-permission-administrator",
      type: Object,
    },

    _dataNeedIdsAndNames: { type: Array },
    _selectedCountry: { type: String },
    _selectedPermissionAdministrator: { type: Object },
    _availablePermissionAdministrators: { type: Array },
    _filteredPermissionAdministrators: { type: Array },
    _availableConnectors: { type: Object },
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
    this._availableConnectors = {};
    this._availablePermissionAdministrators = [];
    this._availableCountries = [];
    this._filteredPermissionAdministrators = [];
    this._dataNeedIdsAndNames = [];

    /**
     * Attributes of the chosen data need.
     * @type {DataNeedAttributes}
     * @private
     */
    this._dataNeedAttributes = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._isValidConfiguration = this.configure();
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

  closePopup() {
    this.dialogRef.value.hide();
  }

  async getRegionConnectorElement() {
    const regionConnectorId =
      this._selectedPermissionAdministrator.regionConnector;
    const regionConnector = this._availableConnectors[regionConnectorId];

    const customElementName = regionConnectorId + "-pa-ce";

    if (!customElements.get(customElementName)) {
      // loaded module needs to have the custom element class as its default export
      try {
        const module = await import(
          `${BASE_URL}/region-connectors/${regionConnector.id}/ce.js`
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

  async handleDataNeedSelect(event) {
    this.dataNeedId = event.target.value;
    this._dataNeedAttributes = await getDataNeedAttributes(this.dataNeedId);

    if (this.isAiida()) {
      this.selectAiida();
    } else {
      this.selectPermissionAdministrator(this._presetPermissionAdministrator);
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

    this._filteredPermissionAdministrators =
      this._availablePermissionAdministrators.filter(
        (pa) =>
          pa.country === this._selectedCountry &&
          this._availableConnectors[pa.regionConnector]
      );

    if (this._filteredPermissionAdministrators.length === 1) {
      this._selectedPermissionAdministrator =
        this._filteredPermissionAdministrators[0];
    }
  }

  async configure() {
    if (!this.dataNeedId && !this.allowDataNeedSelection) {
      console.error(
        "EDDIE button loaded without data-need-id or allow-data-need-selection."
      );
      return false;
    }

    if (this.accountingPointId && !this.permissionAdministratorId) {
      console.error(
        "Accounting point specified without permission administrator."
      );
      return false;
    }

    if (this.allowDataNeedSelection) {
      this._dataNeedIdsAndNames = await fetchJson("/data-needs/api");
    }

    if (this.dataNeedId) {
      this._dataNeedAttributes = await getDataNeedAttributes(this.dataNeedId);

      if (!this._dataNeedAttributes) {
        console.error(`Invalid Data Need ${this.dataNeedId}`);
        return false;
      }
    }

    this._availableConnectors = await getRegionConnectors();
    this._availablePermissionAdministrators = PERMISSION_ADMINISTRATORS.filter(
      (pa) => this._availableConnectors[pa.regionConnector]
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
        console.error(
          `Permission Administrator ${this.permissionAdministratorId} is unavailable.`
        );
        return false;
      }

      this._presetPermissionAdministrator = pa;
      this.selectPermissionAdministrator(pa);
    }

    if (this.isAiida()) {
      if (this.isAiidaEnabled()) {
        this.selectAiida();
      } else {
        console.error(
          "Cannot create an EDDIE connect button for near real-time date because the AIIDA region connector is disabled"
        );
        return false;
      }
    }

    return true;
  }

  isAiida() {
    return (
      this._dataNeedAttributes?.type === "genericAiida" ||
      this._dataNeedAttributes?.type === "smartMeterAiida"
    );
  }

  isAiidaEnabled() {
    return this._availableConnectors.hasOwnProperty("aiida");
  }

  selectAiida() {
    this._selectedCountry = null;
    this._selectedPermissionAdministrator = { regionConnector: "aiida" };
  }

  renderDataNeedSummary() {
    const {
      type,
      purpose,
      policyLink,
      duration,
      minGranularity,
      maxGranularity,
      energyType,
      transmissionInterval,
      dataTags,
    } = this._dataNeedAttributes;
    const [title, details] = DATA_NEED_TOOLTIPS[type];

    return html`
      <sl-alert open>
        <style>
          dl {
            display: grid;
            grid-template-columns: auto 1fr;
          }

          dt {
            font-weight: bold;
          }
        </style>
        <h2>Request for Permission</h2>
        <dl>
          <dt>Type of data</dt>
          <dd>
            ${title}

            <sl-tooltip content="${details}">
              <sl-icon
                name="info-circle"
                style="transform: translateY(1px)"
              ></sl-icon>
            </sl-tooltip>
          </dd>

          <!-- For timeframed data needs -->
          ${duration
            ? html`
                <dt>Duration</dt>
                <dd>
                  ${duration.type === "absolute"
                    ? `From ${new Date(
                        duration.start
                      ).toLocaleDateString()} to ${new Date(
                        duration.start
                      ).toLocaleDateString()}`
                    : `From ${relativeDateFromDuration(
                        duration.start,
                        duration.stickyStartCalendarUnit
                      ).toLocaleDateString()} to ${relativeDateFromDuration(
                        duration.end,
                        duration.stickyStartCalendarUnit
                      ).toLocaleDateString()}`}
                </dd>
              `
            : ""}

          <!-- For validated historical data data needs -->
          ${minGranularity
            ? html`
                <dt>Granularity</dt>
                <dd>
                  ${minGranularity === maxGranularity
                    ? GRANULARITIES[minGranularity]
                    : `${GRANULARITIES[minGranularity]} - ${GRANULARITIES[maxGranularity]}`}
                </dd>
              `
            : ""}
          ${energyType
            ? html`
                <dt>Energy Type</dt>
                <dd>${energyType}</dd>
              `
            : ""}

          <!-- For AIIDA data needs -->
          ${transmissionInterval
            ? html`
                <dt>Transmission Interval</dt>
                <dd>${transmissionInterval} seconds</dd>
              `
            : ""}

          <!-- For AIIDA smart meter data needs -->
          ${dataTags
            ? html`
                <dt>OBIS Points</dt>
                <dd>${dataTags.join(", ")}</dd>
              `
            : ""}

          <dt>Purpose</dt>
          <dd>${purpose}</dd>
        </dl>

        <p>
          By confirming the permission request created from this interaction you
          agree to the
          <a href="${policyLink}" target="_blank">Data Usage Policy</a>
          of the service provider.
        </p>
      </sl-alert>
      <br />
    `;
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
      >
        <div slot="label">${unsafeSVG(headerImage)}</div>

        <!-- Render the data need selection form if the feature is enabled -->
        ${this.allowDataNeedSelection
          ? html`
              <sl-select
                label="Data need specification"
                placeholder="Select a data need"
                @sl-change="${this.handleDataNeedSelect}"
                help-text="The service allows the selection of a data need. This feature is meant for development purposes only."
              >
                ${this._dataNeedIdsAndNames.map(
                  (dataNeed) => html`
                    <sl-option value="${dataNeed.id}"
                      >${dataNeed.name}
                    </sl-option>
                  `
                )}
              </sl-select>
              <br />
            `
          : ""}
        ${this._dataNeedAttributes ? this.renderDataNeedSummary() : ""}

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
