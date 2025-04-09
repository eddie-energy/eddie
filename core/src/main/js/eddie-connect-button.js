import { css, html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";
import { until } from "lit/directives/until.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { choose } from "lit/directives/choose.js";
import { unsafeHTML } from "lit/directives/unsafe-html.js";

// Shoelace
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/dialog/dialog.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/icon/icon.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/button/button.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/select/select.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/option/option.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/divider/divider.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/spinner/spinner.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/tooltip/tooltip.js";

import { setBasePath } from "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/utilities/base-path.js";
import buttonIcon from "../resources/logo.svg?raw";
import headerImage from "../resources/header.svg?raw";

import {
  getDataNeedAttributes,
  getPermissionAdministrators,
  getRegionConnectorMetadata,
  getSupportedRegionConnectors,
} from "./api.js";
import { flagStyles, hasFlag } from "./styles/flags.js";
import { dataNeedDescription } from "./data-need-util.js";

setBasePath("https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn");

const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const CORE_URL =
  import.meta.env.VITE_CORE_URL ?? import.meta.url.split("/lib/")[0];

const SPECIAL_PERMISSION_ADMINISTRATORS = {
  AIIDA: {
    country: "aiida",
    company: "AIIDA",
    name: "AIIDA",
    companyId: "aiida",
    regionConnector: "aiida",
  },
  SIM: {
    country: "sim",
    company: "Simulator",
    name: "Simulator",
    companyId: "sim",
    regionConnector: "sim",
  },
};

/**
 * Maps events dispatched by the button to the view they should navigate to.
 * @type {Map<string, {view: string, step: number, error: boolean?}>}
 */
const eventRoutes = new Map([
  ["eddie-view-data-need", { view: "dn", step: 1 }],
  ["eddie-view-permission-administrator", { view: "pa", step: 2 }],
  ["eddie-view-region-connector", { view: "rc", step: 3 }],
  [
    "eddie-request-unable-to-send",
    { view: "unable-to-send", step: 3, error: true },
  ],
  ["eddie-request-sent-to-permission-administrator", { view: "rc", step: 4 }],
  ["eddie-request-accepted", { view: "accepted", step: 5 }],
  ["eddie-request-rejected", { view: "rejected", step: 5 }],
  ["eddie-request-timed-out", { view: "timed-out", step: 5, error: true }],
  ["eddie-request-invalid", { view: "invalid", step: 5, error: true }],
  [
    "eddie-request-unfulfillable",
    { view: "unfulfillable", step: 5, error: true },
  ],
]);

const dialogOpenEvent = new Event("eddie-dialog-open", {
  bubbles: true,
  composed: true,
});

const dialogCloseEvent = new Event("eddie-dialog-close", {
  bubbles: true,
  composed: true,
});

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
    _activeView: { type: String },
  };

  static styles = [
    flagStyles,
    css`
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
    `,
  ];

  dialogRef = createRef();
  stepIndicatorRef = createRef();

  constructor() {
    super();

    /**
     * Undefined until the configuration has been validated.
     * During configuration, this will be undefined.
     * If the configuration is valid, this will be set to true.
     * If the configuration is invalid, this will be set to false.
     * @type {boolean | undefined}
     * @private
     */
    this._isValidConfiguration = undefined;

    /**
     * Metadata of all enabled region connectors.
     * @type {RegionConnectorMetadata[]}
     * @private
     */
    this._enabledConnectors = [];

    /**
     * Region connectors that support the data need.
     * @type {string[]}
     * @private
     */
    this._supportedConnectors = [];

    /**
     * Permission administrators which region connector supports the data need.
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
     * Country codes which region connectors support the data need.
     * @type {Set<string>}
     * @private
     */
    this._supportedCountries = undefined;

    /**
     * The permission administrator that has been selected by configuration or user input.
     * @type {PermissionAdministrator}
     * @private
     */
    this._selectedPermissionAdministrator = undefined;

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
     * Should only be updated through {@link addViewChangeHandlers}.
     * @type {string}
     * @private
     */
    this._activeView = "dn";
  }

  connectedCallback() {
    super.connectedCallback();

    this.addRequestStatusHandlers();
    this.addViewChangeHandlers();

    // Configure fields that do not depend on public properties
    (async () => {
      this._enabledConnectors = await getRegionConnectorMetadata();
      this._permissionAdministrators = await getPermissionAdministrators();
    })().then(() => {
      this._enabledCountries = this.getEnabledCountries();
      this.reset(); // Start initial configuration
    });
  }

  updated(_changedProperties) {
    super.updated(_changedProperties);

    if (_changedProperties.has("dataNeedId")) {
      this.reset();
    }
  }

  openDialog() {
    this.dialogRef.value.show();
  }

  closeDialog() {
    this.dialogRef.value.hide();
  }

  reset() {
    // Set configuration status to loading
    this._isValidConfiguration = undefined;

    // Reset properties not set during configuration
    this._activeView = "dn";
    this._selectedCountry = undefined;
    this._selectedPermissionAdministrator = undefined;
    this._filteredPermissionAdministrators = [];

    this.configure()
      .then(() => {
        this._isValidConfiguration = true;
      })
      .catch((error) => {
        this._isValidConfiguration = false;
        console.error(error);
      });
  }

  async getRegionConnectorElement() {
    const { country, name, companyId, regionConnector, jumpOffUrl } =
      this._selectedPermissionAdministrator;

    const baseUrl = `${CORE_URL}/region-connectors/${regionConnector}`;
    const elementUrl = `${baseUrl}/ce.js`;

    console.debug(`Loading region connector element for ${regionConnector}`);

    const customElementName = regionConnector + "-pa-ce";

    if (!customElements.get(customElementName)) {
      // loaded module needs to have the custom element class as its default export
      try {
        const module = await import(/* @vite-ignore */ elementUrl);
        customElements.define(customElementName, module.default);
      } catch (error) {
        // If multiple EDDIE buttons are preconfigured with the same region connector,
        // they may define its custom element at the same time.
        // This will cause an error, but it can be safely ignored.
        if (!customElements.get(customElementName)) {
          throw new Error(error);
        }
      }
    }

    const element = document.createElement(customElementName);
    element.setAttribute("core-url", CORE_URL);
    element.setAttribute("base-url", baseUrl);
    element.setAttribute("connection-id", this.connectionId);
    element.setAttribute("data-need-id", this.dataNeedId);
    element.setAttribute("country-code", country);
    element.setAttribute("jump-off-url", jumpOffUrl);
    element.setAttribute("company-id", companyId);
    element.setAttribute("company-name", name);

    if (this.accountingPointId) {
      element.setAttribute("accounting-point-id", this.accountingPointId);
    }

    return html`
      <h3>
        Follow the instructions for ${name}
        ${hasFlag(country)
          ? html`<span class="flag flag-${country}"></span>`
          : ""}
      </h3>

      <eddie-notification-handler>
        <eddie-request-status-handler>
          <div>${element}</div>
        </eddie-request-status-handler>
      </eddie-notification-handler>
    `;
  }

  /**
   * Returns the country codes of all countries that are supported by at least one enabled region connector.
   * @returns {string[]} - The country codes of all enabled region connectors in lowercase.
   */
  getEnabledCountries() {
    return [
      ...new Set(
        this._enabledConnectors
          .flatMap((rc) => rc.countryCodes)
          .map((countryCode) => countryCode.toLowerCase())
      ),
    ]
      .filter((country) =>
        new Set(this._permissionAdministrators.map((pa) => pa.country)).has(
          country
        )
      )
      .sort((a, b) => a.localeCompare(b));
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

  handleDataNeedConfirmed() {
    if (this.isAiida()) {
      this.dispatchEvent(new Event("eddie-view-region-connector"));
    } else {
      this.dispatchEvent(new Event("eddie-view-permission-administrator"));
    }
  }

  handlePermissionAdministratorSelected() {
    this.dispatchEvent(new Event("eddie-view-region-connector"));
  }

  /**
   * Configures the dialog to select the given permission administrator.
   * @param permissionAdministrator {PermissionAdministrator}
   */
  selectPermissionAdministrator(permissionAdministrator) {
    this.selectCountry(permissionAdministrator?.country);
    this._selectedPermissionAdministrator = permissionAdministrator;
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

  selectCountry(country) {
    this._selectedPermissionAdministrator = undefined;
    this._selectedCountry = country;

    if (country === "sim") {
      this._selectedPermissionAdministrator =
        SPECIAL_PERMISSION_ADMINISTRATORS.SIM;
    }

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
      throw new Error(`No data need is available for id ${this.dataNeedId}`);
    }

    if (!this._dataNeedAttributes.enabled) {
      return; // No more configuration needed
    }

    if (this._enabledConnectors.length === 0) {
      throw new Error("No enabled region connectors.");
    }

    this._supportedConnectors = await getSupportedRegionConnectors(
      this.dataNeedId
    );

    if (this._supportedConnectors.length === 0) {
      throw new Error("No region connector supports the data need.");
    }

    this._supportedPermissionAdministrators =
      this._permissionAdministrators.filter((pa) =>
        this._supportedConnectors.includes(pa.regionConnector)
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

      this.selectPermissionAdministrator(pa);
    }

    if (this.isAiida()) {
      this.configureAiida();
    }

    if (
      !this.permissionAdministratorId &&
      this.rememberPermissionAdministrator
    ) {
      this.loadPermissionAdministratorFromLocalStorage();
    }
  }

  isAiida() {
    return this._dataNeedAttributes?.type === "aiida";
  }

  configureAiida() {
    if (!this._enabledConnectors.some((rc) => rc.id === "aiida")) {
      throw new Error(
        `Data need with id ${this.dataNeedId} is an AIIDA data need, but AIIDA is not enabled.`
      );
    }

    if (!this._supportedConnectors.includes("aiida")) {
      throw new Error(
        `AIIDA does not support the data need with id ${this.dataNeedId}.`
      );
    }

    this._selectedPermissionAdministrator =
      SPECIAL_PERMISSION_ADMINISTRATORS.AIIDA;
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
  }

  simIsEnabled() {
    return this._enabledConnectors.some((rc) => rc.id === "sim");
  }

  addViewChangeHandlers() {
    for (const [event, { view, step, error }] of eventRoutes) {
      this.addEventListener(event, () => {
        if (view) {
          this._activeView = view;
        }

        if (step) {
          this.stepIndicatorRef.value.step = step;
        }

        this.stepIndicatorRef.value.error = !!error;
      });
    }
  }

  dataNeedDescription() {
    return unsafeHTML(dataNeedDescription(this._dataNeedAttributes));
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

    if (!this._dataNeedAttributes.enabled) {
      return html`
        <button class="eddie-connect-button" disabled>
          ${unsafeSVG(buttonIcon)}
          <span>Disabled Configuration</span>
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
        <eddie-step-indicator
          ${ref(this.stepIndicatorRef)}
          step="1"
        ></eddie-step-indicator>

        ${choose(this._activeView, [
          [
            "dn",
            () => html`
              <h3>Confirm the data usage policy</h3>
              <data-need-summary
                data-need-id="${this.dataNeedId}"
              ></data-need-summary>

              <br />
              <sl-button
                @click="${this.handleDataNeedConfirmed}"
                variant="primary"
                style="float: right"
              >
                Continue
              </sl-button>
            `,
          ],
          [
            "pa",
            () => html`
              <h3>Select your country and permission administrator</h3>

              <!-- Render country selection -->
              <sl-select
                label="Country"
                placeholder="Select your country"
                @sl-change="${(event) =>
                  this.selectCountry(event.target.value)}"
                value="${this._selectedCountry}"
                ?disabled="${this.permissionAdministratorId}"
                help-text="${this._supportedCountries.size !==
                this._enabledCountries.length
                  ? "Some countries do not support the given data requirements."
                  : ""}"
              >
                ${hasFlag(this._selectedCountry)
                  ? html`<span
                      slot="prefix"
                      class="flag flag-${this._selectedCountry}"
                    ></span>`
                  : ""}
                ${this._enabledCountries.map(
                  (country) => html`
                    <sl-option
                      value="${country}"
                      ?disabled="${!this._supportedCountries.has(country)}"
                    >
                      ${hasFlag(country)
                        ? html`<span
                            slot="prefix"
                            class="flag flag-${country}"
                          ></span>`
                        : ""}
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

              <!-- Render permission administrator selection -->
              <sl-select
                label="Permission Administrator"
                placeholder="Select your Permission Administrator"
                help-text="Typically your Distribution System Operator (DSO) or Transmission System Operator (TSO), depending on your national regulation."
                @sl-change="${this.handlePermissionAdministratorSelect}"
                value="${ifDefined(
                  this._selectedPermissionAdministrator?.companyId
                )}"
                ?disabled="${!this._selectedCountry ||
                this.permissionAdministratorId ||
                this._filteredPermissionAdministrators.length <= 1}"
              >
                ${this._filteredPermissionAdministrators.map(
                  (pa) => html`
                    <sl-option value="${pa.companyId}">${pa.company}</sl-option>
                  `
                )}
              </sl-select>

              <br />
              <sl-button
                @click="${() =>
                  this.dispatchEvent(new Event("eddie-view-data-need"))}"
              >
                <sl-icon name="arrow-left"></sl-icon>
              </sl-button>
              <sl-button
                @click="${this.handlePermissionAdministratorSelected}"
                ?disabled="${!this._selectedPermissionAdministrator}"
                variant="primary"
                style="float: right"
              >
                Continue
              </sl-button>
            `,
          ],
          [
            "rc",
            () => html`
              <!-- RCs are always one step since it is not possible to navigate their contents -->
              ${until(
                this.getRegionConnectorElement().catch((error) => {
                  console.error(error);
                  this.stepIndicatorRef.value.error = true;
                  return html`
                    <h3>Error loading region connector</h3>

                    <sl-alert variant="danger" open>
                      <sl-icon
                        slot="icon"
                        name="exclamation-triangle"
                      ></sl-icon>
                      We were unable to communicate with of our service that
                      handles permission requests for
                      ${this._selectedPermissionAdministrator?.company}. Please
                      contact the customer support of the service provider.
                    </sl-alert>
                  `;
                }),
                html`
                  <h3>Loading region connector</h3>

                  <sl-alert open>
                    <sl-spinner slot="icon"></sl-spinner>

                    <p>
                      We are loading our service that handles permission
                      requests for
                      ${this._selectedPermissionAdministrator?.name}. This may
                      take a moment.
                    </p>
                  </sl-alert>
                `
              )}

              <br />
              <sl-button
                @click="${() =>
                  this.isAiida()
                    ? this.dispatchEvent(new Event("eddie-view-data-need"))
                    : this.dispatchEvent(
                        new Event("eddie-view-permission-administrator")
                      )}"
              >
                <sl-icon name="arrow-left"></sl-icon>
              </sl-button>
            `,
          ],
          [
            "unable-to-send",
            () => html`
              <h3>Unable to send permission request</h3>

              <sl-alert variant="danger" open>
                <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>

                <p>
                  We were unable to send the permission request to your
                  permission administrator. Please contact the customer support
                  of the service provider.
                </p>
              </sl-alert>

              <br />
              <sl-button
                variant="danger"
                @click="${this.closeDialog}"
                style="float: right"
              >
                Close
              </sl-button>
            `,
          ],
          [
            "accepted",
            () => html`
              <h3>Permission granted</h3>

              <sl-alert variant="success" open>
                <sl-icon slot="icon" name="check-circle"></sl-icon>

                <p>
                  You successfully granted permission for the service provider
                  to access to your ${this.dataNeedDescription()}. The
                  permission can be terminated by either party at any time.
                </p>

                <p>
                  You may now close this dialog and continue on the website of
                  the service provider.
                </p>
              </sl-alert>

              <br />
              <sl-button
                variant="success"
                @click="${this.closeDialog}"
                style="float: right"
              >
                Close
              </sl-button>
            `,
          ],
          [
            "rejected",
            () => html`
              <h3>Permission request rejected</h3>

              <sl-alert variant="primary" open>
                <sl-icon slot="icon" name="info-circle"></sl-icon>

                <p>
                  You rejected the permission request for the service provider
                  to access to your ${this.dataNeedDescription()}. No data will
                  be processed.
                </p>

                <p>
                  You may now close this dialog. Please start again if the
                  request was rejected unintentionally.
                </p>
              </sl-alert>

              <br />
              <sl-button
                variant="primary"
                @click="${this.closeDialog}"
                style="float: right"
              >
                Close
              </sl-button>
            `,
          ],
          [
            "timed-out",
            () => html`
              <h3>Permission request timed out</h3>

              <sl-alert variant="danger" open>
                <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
                <p>
                  The permission request was not accepted in the expected
                  timeframe. No data will be processed.
                </p>

                <p>
                  You may close this dialog and start again. Please contact the
                  service provider if the permission request does not show up in
                  the portal of your permission administrator.
                </p>
              </sl-alert>

              <br />
              <sl-button
                variant="danger"
                @click="${this.closeDialog}"
                style="float: right"
              >
                Close
              </sl-button>
            `,
          ],
          [
            "invalid",
            () => html`
              <h3>Request was declined as invalid</h3>

              <sl-alert variant="danger" open>
                <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
                <p>
                  Your permission administrator declined our permission request
                  as invalid.
                </p>

                <p>
                  You may close this dialog and start again. Please contact the
                  service provider if the issue persists.
                </p>
              </sl-alert>

              <br />
              <sl-button
                variant="danger"
                @click="${this.closeDialog}"
                style="float: right"
              >
                Close
              </sl-button>
            `,
          ],
          [
            "unfulfillable",
            () => html`
              <h3>Unable to fulfill the request</h3>

              <sl-alert variant="danger" open>
                <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
                <p>
                  Your energy data provider is unable to provide the requested
                  ${this.dataNeedDescription()}. No data will be processed.
                </p>

                <p>
                  You may now close this dialog and continue on the website of
                  the service provider.
                </p>
              </sl-alert>

              <br />
              <sl-button
                variant="danger"
                @click="${this.closeDialog}"
                style="float: right"
              >
                Close
              </sl-button>
            `,
          ],
        ])}

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
