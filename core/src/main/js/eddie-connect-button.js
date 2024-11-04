import { css, html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";
import { until } from "lit/directives/until.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { choose } from "lit/directives/choose.js";

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

import PERMISSION_ADMINISTRATORS from "../../../../european-masterdata/src/main/resources/permission-administrators.json";

setBasePath("https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn");

const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const COUNTRIES = new Set(PERMISSION_ADMINISTRATORS.map((pa) => pa.country));

const CORE_URL =
  import.meta.env.VITE_CORE_URL ??
  import.meta.url.replace("/lib/eddie-components.js", "");

const eventRoutes = new Map([
  ["eddie-data-need-confirmed", { view: "pa" }],
  ["eddie-permission-administrator-selected", { view: "rc" }],
  ["eddie-request-unable-to-send", { view: "unable-to-send" }],
  ["eddie-request-accepted", { view: "accepted" }],
  ["eddie-request-rejected", { view: "rejected" }],
  ["eddie-request-timed-out", { view: "timed-out" }],
  ["eddie-request-invalid", { view: "invalid" }],
  ["eddie-request-unfulfillable", { view: "unfulfillable" }],
]);

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
    _activeView: { type: String },
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

    /**
     * Always use {@link navigateToView} to update this value.
     * @type {string}
     * @private
     */
    this._activeView = "dn";
  }

  connectedCallback() {
    super.connectedCallback();

    this.configure()
      .then(() => {
        this._isValidConfiguration = true;

        this.addRequestStatusHandlers();
        this.addViewChangeHandlers();
      })
      .catch(() => (this._isValidConfiguration = false));
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
    console.debug(`Loading region connector for ${regionConnectorId}`);

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

          return this.renderRegionConnectorError();
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

    return html`
      <h3>Follow the instructions for your region</h3>

      <eddie-notification-handler>
        <eddie-request-status-handler>
          <div>${element}</div>
        </eddie-request-status-handler>
      </eddie-notification-handler>

      <br />
      <sl-button
        @click="${() =>
          this.isAiida()
            ? this.navigateToView("dn")
            : this.navigateToView("pa")}"
      >
        <sl-icon name="arrow-left"></sl-icon>
      </sl-button>
    `;
  }

  renderRegionConnectorError() {
    // TODO: Configure EP contact email: <a href="mailto:support@ep.local">support@ep.local</a>
    return html`<h3>Error loading region connector</h3>
      <sl-alert variant="danger" open>
        <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
        We were unable to communicate with of our service that handles
        permission requests for
        ${this._selectedPermissionAdministrator.company}. Please contact the
        customer support of the service provider.
      </sl-alert>`;
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
      this.navigateToView("rc");
    } else {
      this.navigateToView("pa");
    }
  }

  handlePermissionAdministratorSelected() {
    this.navigateToView("rc");
  }

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
    this._selectedPermissionAdministrator = null;
    this._selectedCountry = country;

    if (country === "sim") {
      this._selectedPermissionAdministrator = { regionConnector: "sim" };
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

  addViewChangeHandlers() {
    for (const [event, { view }] of eventRoutes) {
      this.addEventListener(event, () => {
        this.navigateToView(view);
      });
    }

  navigateToView(view) {
    this._activeView = view;
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
                ?disabled="${ifDefined(this._presetPermissionAdministrator)}"
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
                !!this._presetPermissionAdministrator ||
                this._filteredPermissionAdministrators.length <= 1}"
              >
                ${this._filteredPermissionAdministrators.map(
                  (pa) => html`
                    <sl-option value="${pa.companyId}">${pa.company}</sl-option>
                  `
                )}
              </sl-select>

              <br />
              <sl-button @click="${() => this.navigateToView("dn")}">
                <sl-icon name="arrow-left"></sl-icon>
              </sl-button>
              <sl-button
                @click="${this.handlePermissionAdministratorSelected}"
                ?disabled="${!this._selectedPermissionAdministrator}"
                variant="primary"
              >
                Continue
              </sl-button>
            `,
          ],
          [
            "rc",
            () => html`
              <!-- RCs are always one step since it is not possible to navigate their contents -->
              ${this._selectedPermissionAdministrator
                ? html`
                    ${until(
                      this.getRegionConnectorElement(),
                      html`<sl-spinner></sl-spinner>`
                    )}
                  `
                : ""}
            `,
          ],
          [
            "unable-to-send",
            () => html`
              <h3>Unable to send permission request</h3>
              <p>
                We were unable to send the permission request to your permission
                administrator. Please contact the customer support of the
                service provider.
              </p>

              <sl-button @click="${this.closeDialog}">Close</sl-button>
            `,
          ],
          [
            "accepted",
            () => html`
              <h3>Permission granted</h3>
              <p>
                You successfully granted permission for the service provider to
                access to your data. The permission can be terminated by either
                party at any time.
              </p>

              <p>
                You may now close this dialog and continue on the website of the
                service provider.
              </p>

              <sl-button @click="${this.closeDialog}">Close</sl-button>
            `,
          ],
          [
            "rejected",
            () => html`
              <h3>Permission request rejected</h3>
              <p>
                You rejected the permission request for the service provider to
                access to your data. No data will be processed.
              </p>

              <p>
                You may now close this dialog. Please start again if the request
                was rejected unintentionally.
              </p>

              <sl-button @click="${this.closeDialog}">Close</sl-button>
            `,
          ],
          [
            "timed-out",
            () => html`
              <h3>Permission request timed out</h3>
              <p>
                The permission request was not accepted in the expected
                timeframe. No data will be processed.
              </p>

              <p>
                You may close this dialog and start again. Please contact the
                service provider if the permission request does not show up in
                the portal of your permission administrator.
              </p>

              <sl-button @click="${this.closeDialog}">Close</sl-button>
            `,
          ],
          [
            "invalid",
            () => html`
              <h3>Request was declined as invalid</h3>
              <p>
                Your permission administrator declined our permission request as
                invalid.
              </p>

              <p>
                You may close this dialog and start again. Please contact the
                service provider if the issue persists.
              </p>

              <sl-button @click="${this.closeDialog}">Close</sl-button>
            `,
          ],
          [
            "unfulfillable",
            () => html`
              <h3>Unable to fulfill the request</h3>
              <p>
                Your energy data provider is unable to provide the requested
                data. No data will be processed.
              </p>

              <p>
                You may now close this dialog and continue on the website of the
                service provider.
              </p>

              <sl-button @click="${this.closeDialog}">Close</sl-button>
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
