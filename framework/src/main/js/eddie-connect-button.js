import { css, html, LitElement } from "lit";
import { createRef, ref } from "lit/directives/ref.js";
import { until } from "lit/directives/until.js";
import { unsafeSVG } from "lit/directives/unsafe-svg.js";

import eddieLogo from "../resources/eddie-logo.svg?raw";

import PERMISSION_ADMINISTRATORS from "../resources/permission-administrators.json";

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

    .eddie-connect-button::before {
      content: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' xml:space='preserve' id='Layer_1' x='0' y='0' style='enable-background:new 0 0 1500 1497.9' version='1.1' viewBox='0 0 1500 1497.9'%3E%3Cstyle%3E .st2%7Bfill:%23ffd208%7D.st3%7Bfill:%23ea5297%7D.st4%7Bfill:%23007aa2%7D.st5%7Bfill:%2394c242%7D %3C/style%3E%3Cpath d='M1157.8 724.9c36.3-16.9 62.3-13.6 84.5-1.2-13.8-110.4-60.6-149.1-74.8-158.5-36 22.8-64.8 55.8-82.4 95-27.9 35.9-187.2 47.3-325.4 50.2-4.1.1-8.4.2-12.8.2-46.7.8-90.3.7-124.8.4-21.3-.6-42.8-1.6-63.4-3.4C592.4 720 648.6 746.4 661 788c26.2-.1 55.4.1 85.9.6 4.4.1 8.7.2 12.8.2 138.5 2.9 298.3 14.3 325.6 50.4-4.2-34.5 4.8-82.7 72.5-114.3z' style='fill:%231da7e0'/%3E%3Cpath d='M691.2 1162c94.4 110.7 202.4 110.4 275.6 98.2v-.2c-.6-6.6-1.6-13.1-2.8-19.5-.1-.4-.1-.8-.2-1.2-1.3-6.4-2.8-12.7-4.6-18.9-.2-.5-.3-1-.4-1.6-1.8-6.1-3.9-12.1-6.3-18-.3-.7-.5-1.4-.8-2.1-2.4-5.8-5-11.4-7.9-16.9l-1.2-2.4c-2.9-5.5-6.1-10.8-9.5-16.1-.5-.8-1-1.5-1.5-2.3-3.6-5.4-7.5-10.7-11.6-15.8-.4-.5-.7-.9-1.1-1.4-7.2-8.8-15.1-17-23.6-24.6-2.3-2-4.6-4-7-6-.5-.4-.9-.7-1.4-1.1-5.1-4.1-10.4-8-15.8-11.6-.7-.5-1.5-.9-2.2-1.4-5.2-3.4-10.6-6.6-16.1-9.5l-2.4-1.2c-5.5-2.9-11.2-5.5-17-7.9-.7-.3-1.3-.5-2-.8-5.9-2.4-12-4.5-18.1-6.3-.5-.1-.9-.3-1.4-.4-6.2-1.8-12.6-3.4-19.1-4.7-.4-.1-.7-.1-1.1-.2-6.5-1.2-13-2.2-19.7-2.8-.3 0-.6 0-.9-.1-6.6-.6-13.4-1-20.2-1-6.8 0-13.5.4-20.2 1-.3 0-.6 0-.9.1-6.7.6-13.2 1.6-19.7 2.8-.2 0-.4 0-.6.1-.1 0-.3.1-.4.1-1.1.2-2.2.4-3.4.7-6.9 1.2-23.1 3-46.1-.9 2 2.4 4 5 6.1 7.6 17.4 22.1 20 44 25.5 86.3z' class='st2'/%3E%3Cpath d='M691.2 1162c1.9 14.5 4.2 31.4 7.5 51.5 28.1 168.4 144.7 212.7 202 224.3 1.2-1.1 2.4-2.2 3.6-3.4 4.4-4.4 8.6-9.1 12.6-13.8.4-.5.9-1 1.3-1.6.2-.2.3-.4.5-.7.4-.5.7-.9 1.1-1.4 4.1-5.1 8-10.3 11.6-15.8.5-.8 1-1.5 1.5-2.3 3.4-5.2 6.6-10.6 9.5-16.1l1.2-2.4c2.8-5.5 5.5-11.1 7.9-16.9.3-.7.5-1.4.8-2.1 2.3-5.9 4.5-11.9 6.3-18 .2-.5.3-1 .4-1.6 1.8-6.2 3.4-12.5 4.6-18.9.1-.4.1-.8.2-1.2 1.2-6.4 2.2-12.9 2.8-19.5 0-.4.1-.7.1-1.1.6-6.6 1-13.2 1-20v-.1c0-6.7-.4-13.4-1-20 0-.3 0-.6-.1-.9-72.9 12.4-181 12.6-275.4-98z' class='st3'/%3E%3Cpath d='M478.3 1071.1c6.9 7.7 10.9 12.4 10.9 12.4 57.2 76.4 52.5 135.8 49.8 151.8-.3 1.3-.6 2.7-.8 4-.1.3-.1.7-.2 1-1.2 6.5-2.2 13-2.8 19.7 0 .3 0 .6-.1.9-.6 6.6-1 13.4-1 20.2 0 6.8.4 13.5 1 20.2 0 .3 0 .6.1.9.6 6.7 1.6 13.2 2.8 19.7.1.3.1.7.2 1 1.3 6.5 2.8 12.9 4.7 19.1.1.4.2.9.4 1.3 1.9 6.2 4 12.3 6.4 18.2.2.6.5 1.3.7 1.9 2.4 5.8 5.1 11.5 7.9 17.1.4.7.8 1.5 1.1 2.2 3 5.6 6.2 11 9.6 16.3.5.7.9 1.4 1.3 2.1 3.7 5.5 7.6 10.8 11.7 16 .3.4.6.8 1 1.2 9.2 11.2 19.4 21.5 30.7 30.7.4.3.8.6 1.2 1 5.1 4.1 10.5 8.1 16 11.7.7.5 1.4.9 2.1 1.3 5.3 3.4 10.7 6.6 16.3 9.6.7.4 1.5.8 2.2 1.1 5.6 2.9 11.3 5.5 17.1 7.9.6.3 1.3.5 1.9.7 5.9 2.4 12 4.5 18.2 6.4.4.1.9.2 1.3.4 6.3 1.8 12.7 3.4 19.1 4.7.3.1.7.1 1 .2 6.5 1.2 13 2.2 19.7 2.8.3 0 .6 0 .9.1 6.6.6 13.4 1 20.2 1 6.8 0 13.5-.4 20.2-1 .3 0 .6 0 .9-.1 6.7-.6 13.2-1.6 19.7-2.8.4-.1.7-.1 1.1-.2 6.5-1.3 12.8-2.8 19.1-4.7.5-.1.9-.3 1.4-.4 6.2-1.8 12.2-4 18.1-6.3.7-.3 1.3-.5 2-.8 5.8-2.4 11.5-5 17-7.9.8-.4 1.6-.8 2.3-1.2 5.5-2.9 10.9-6.1 16.1-9.5.7-.5 1.5-.9 2.2-1.4 5.5-3.6 10.7-7.5 15.8-11.6.4-.4.9-.7 1.4-1.1 4.3-3.5 8.4-7.3 12.5-11.1-57.3-11.6-174-55.9-202-224.3-3.4-20.1-5.6-37-7.5-51.5-5.6-42.2-8.1-64.2-25.4-86.2-2.1-2.7-4.1-5.2-6.1-7.6-27.4-4.6-64.2-17.3-106.3-48.8 0 0-75.5-64.3-114.4-121.1-37.7 37.4 66.5 105.5 39.3 172.8z' class='st4'/%3E%3Cpath d='M269.4 874C137.9 870.6 45.7 795.5.7 731.7c-4.7 61 16.2 123.6 62.8 170.2 28.4 28.4 62.6 47 98.8 56.3 3.2.9 6.4 1.6 9.7 2.4 1.6.3 3.3.7 4.9 1 13.6 2.7 27.6 4.1 42 4.1 31 0 60.4-6.5 87.1-18.3 44.6-13 140.1 87.7 172.3 123.6 27.2-67.3-77.1-135.4-39.2-173-12.8-18.7-21.6-36.7-22.5-51.2l-.8.9c.1.1-48.5 28.8-146.4 26.3z' class='st3'/%3E%3Cpath d='M558.8 707.6c-71.1-6.1-132-20.7-142.3-53.8 0-.4.1-.9.1-1.3-14.8 11.5-91.4 65.8-233.1 74.9 0 0-106.9 10.1-173.9-42.6-4.7 15.3-7.6 31-8.9 46.9C45.7 795.5 138 870.6 269.5 874c97.8 2.5 146.4-26.1 146.4-26.1l.8-.9c0-.5-.1-1-.1-1.5 13.4-42.7 110.6-54.6 205.6-57.1 11.9-.1 25-.2 38.9-.3-12.4-41.7-68.7-68.2-102.3-80.5z' class='st2'/%3E%3Cpath d='M183.4 727.4c141.7-9.1 218.3-63.4 233.1-74.9 1.2-58.3 136.9-173.8 136.9-173.8 76.4-57.2 135.8-52.5 151.8-49.8l4.2.9c.2 0 .5.1.7.1 6.6 1.3 13.2 2.2 20 2.9h.5c6.7.6 13.5 1 20.4 1 6.9 0 13.7-.4 20.4-1h.5c6.8-.6 13.4-1.6 20-2.9.2 0 .5-.1.7-.1 6.6-1.3 13.1-2.9 19.4-4.7.3-.1.7-.2 1-.3 6.3-1.9 12.4-4 18.5-6.5.5-.2 1.1-.4 1.6-.6 5.9-2.4 11.7-5.1 17.4-8.1.7-.3 1.3-.7 2-1 5.7-3 11.1-6.3 16.5-9.7.6-.4 1.3-.8 1.9-1.2 5.6-3.7 11-7.7 16.2-11.9.3-.3.7-.5 1-.8 11.3-9.2 21.6-19.5 30.8-30.8.3-.3.5-.7.8-1 4.2-5.2 8.2-10.6 11.9-16.1.4-.6.8-1.3 1.3-2 3.5-5.3 6.7-10.8 9.7-16.4.4-.7.7-1.4 1.1-2.1 2.9-5.6 5.6-11.4 8-17.2.2-.6.5-1.2.7-1.8 2.4-6 4.5-12.1 6.4-18.3.1-.4.2-.8.4-1.2 1.8-6.3 3.4-12.7 4.7-19.2.1-.3.1-.6.2-1 1.2-6.5 2.2-13.1 2.9-19.8 0-.3 0-.6.1-.8.6-6.7 1-13.4 1-20.2 0-6.8-.4-13.5-1-20.2 0-.3 0-.6-.1-.9-.6-6.7-1.6-13.2-2.8-19.7-.1-.4-.1-.7-.2-1.1v-.2c-8.4-13.2-32.8-26.7-103-4-87.5 28.3-110.1 160.8-329.1 163.3-7.3 23.1-20.1 50.3-42.5 80.2 0 0-129.1 151.7-183.2 136-15.1-6.6-31.1-11.6-47.7-14.6h-.1c-5.7-1-11.5-1.9-17.4-2.5-63.2-7-128.8 13.6-177.3 62.1-25.9 25.9-43.9 56.8-53.9 89.6 66.8 52.4 173.6 42.3 173.6 42.3z' class='st5'/%3E%3Cpath d='M963.8 174.9c-1.3-6.4-2.8-12.7-4.6-18.9-.1-.5-.3-.9-.4-1.4-1.8-6.2-4-12.2-6.3-18.1-.3-.7-.5-1.3-.8-2-2.4-5.8-5-11.5-7.9-17-.4-.8-.8-1.6-1.2-2.3-2.9-5.5-6.1-10.9-9.5-16.1-.5-.7-.9-1.5-1.4-2.2-3.6-5.5-7.5-10.7-11.6-15.8-.4-.4-.7-.9-1.1-1.4-4.6-5.6-9.4-11-14.5-16.1-4.4-4.4-9.1-8.6-13.8-12.6-.5-.4-1-.9-1.6-1.3-.2-.2-.4-.3-.7-.5-.5-.4-.9-.7-1.4-1.1-5.1-4.1-10.3-8-15.8-11.6-.8-.5-1.5-1-2.3-1.5-3-1.9-6-3.8-9.1-5.6-141.5 21-159.2 138.7-159.2 138.7-35.8 112-128.6 121.1-160.4 120.8-.6 12.1-2.9 27.5-8.5 45.3 219-2.5 241.6-135 329.1-163.3 70.2-22.8 94.5-9.2 103 4z' class='st4'/%3E%3Cpath d='M700.6 168.1S718.3 50.4 859.7 29.3c-2.3-1.3-4.6-2.7-7-3.9l-2.4-1.2c-5.5-2.8-11.1-5.5-16.9-7.9-.7-.3-1.4-.5-2.1-.8-5.9-2.3-11.9-4.5-18-6.3-.5-.2-1-.3-1.6-.4-6.2-1.8-12.5-3.4-18.9-4.6-.4-.1-.8-.1-1.2-.2-6.4-1.2-12.9-2.2-19.5-2.8-.3-.2-.7-.2-1.1-.2-6.6-.6-13.2-1-20-1h-.1c-6.7 0-13.4.4-20 1-.4 0-.7 0-1.1.1-6.6.6-13.1 1.6-19.5 2.8-.4.1-.8.1-1.2.2-6.4 1.3-12.7 2.8-18.9 4.6-.5.2-1 .3-1.6.4-6.1 1.8-12.1 3.9-18 6.3-.7.3-1.4.5-2.1.8-5.8 2.4-11.4 5-16.9 7.9l-2.4 1.2c-5.5 2.9-10.8 6.1-16.1 9.5-.8.5-1.5 1-2.3 1.5-5.4 3.6-10.7 7.5-15.8 11.6-.5.4-.9.7-1.4 1.1-8.8 7.2-17 15.1-24.6 23.6-2 2.3-4 4.6-6 7-.4.5-.7.9-1.1 1.4-4.1 5.1-8 10.4-11.6 15.8-.5.7-.9 1.5-1.4 2.2-3.4 5.2-6.6 10.6-9.5 16.1l-1.2 2.4c-2.9 5.5-5.5 11.2-7.9 17-.3.7-.5 1.3-.8 2-2.4 5.9-4.5 12-6.3 18.1-.1.5-.3.9-.4 1.4-1.8 6.2-3.4 12.6-4.7 19.1-.1.4-.1.7-.2 1.1-1.2 6.5-2.2 13-2.8 19.7 0 .3 0 .6-.1.9-.6 6.6-1 13.4-1 20.2 0 6.8.4 13.5 1 20.2 0 .3 0 .6.1.8.6 6.7 1.6 13.3 2.8 19.7 0 .2 0 .4.1.6 0 .1.1.2.1.4.2 1.1.4 2.3.7 3.4.8 4.8 2 14.1 1.3 26.9 31.9.2 124.7-8.9 160.5-120.9z' class='st3'/%3E%3Cpath d='M1242.3 723.7c48.7 27.2 78.5 98 155.7 94.6 76.7-3.4 96.8-49.6 101.8-78.6-4.6-115.7-99.8-208-216.6-208-42.5 0-82.2 12.3-115.6 33.4 14.1 9.5 60.9 48.2 74.7 158.6z' class='st2'/%3E%3Cpath d='M1485.6 826.3c1.7-4.4 3.3-8.9 4.7-13.5.6-1.8 1-3.6 1.5-5.4 1.4-5.1 2.8-10.4 3.8-15.6.3-1.3.4-2.5.7-3.8 1.1-5.8 1.9-11.6 2.5-17.5.1-.7.1-1.4.2-2.1.6-6.3.9-12.7.9-19.2v-.4c0-3-.1-5.9-.2-8.8-5 29-25.1 75.2-101.8 78.6-77.2 3.4-107-67.5-155.7-94.6-22.2-12.4-48.3-15.7-84.5 1.2-67.7 31.6-76.7 79.8-72.4 114.3.9 1.2 1.6 2.4 2.2 3.6l.1-.1c.3.5.5 1.1.8 1.6 2.3 4.6 4.7 9.1 7.3 13.6.8 1.4 1.6 2.8 2.5 4.2 3.1 5 6.4 9.9 9.9 14.7l1.8 2.4c3.7 4.9 7.6 9.6 11.7 14.2.6.6 1.1 1.3 1.7 1.9 2.2 2.3 4.1 4.8 6.4 7.1 2.4 2.4 4.9 4.4 7.4 6.7l.3.3c82.4 75 208.4 75.1 290.9.3.3-.3.6-.5.8-.8 2.4-2.2 4.9-4.2 7.2-6.5 2.2-2.2 4.1-4.6 6.2-6.8.8-.8 1.5-1.7 2.3-2.5 3.9-4.4 7.7-8.9 11.2-13.6.8-1.1 1.6-2.2 2.4-3.2 3.3-4.5 6.4-9.1 9.3-13.8 1.1-1.7 2.1-3.5 3.1-5.3 2.4-4.1 4.6-8.2 6.7-12.5 1.6-3.2 3.1-6.5 4.6-9.9 1.3-3.2 2.4-6 3.5-8.8z' class='st5'/%3E%3C/svg%3E%0A");
      display: block;
      width: 1.75rem;
      height: 1.75rem;
    }

    .eddie-connect-dialog {
      background: white;
      border: none;
      border-radius: 1em;
      width: max(400px, 50%);
      max-height: calc(100% - 4rem);
      max-width: calc(100% - 4rem);
      overflow-x: hidden;
    }

    .eddie-connect-dialog::backdrop {
      background-color: #00092288;
    }

    .dialog-content {
      margin-top: 1rem;
      padding: 0.5rem;
      display: grid;
      justify-items: start;
    }

    label {
      font-weight: bold;
      margin-bottom: 0.25rem;
    }

    select {
      border: 1px solid #c0ccda;
      border-radius: 3px;
      margin-bottom: 1rem;
      max-width: 100%;
      appearance: none;
      background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 286.1 167'%3E%3Cpath d='M24.1 0h238c21.4 0 32.1 25.9 17 41l-119 119c-9.4 9.4-24.6 9.4-33.9 0L7.1 41C-8 25.9 2.7 0 24.1 0z' fill='%233c4858'/%3E%3C/svg%3E");
      background-position: right 0.65em top 50%;
      background-repeat: no-repeat;
      background-size: 0.65em auto;
      padding: 0.5rem 1.8rem 0.5rem 0.5rem;
    }

    .dialog-close {
      position: absolute;
      right: 1rem;
      top: 1rem;
      border: none;
      border-radius: 100%;
      background: #017aa0;
      color: white;
      width: 2rem;
      height: 2rem;
      display: grid;
      place-content: center;
      font-size: 1.5rem;
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

    this.dialogRef.value.showModal();
  }

  closePopup() {
    this.dialogRef.value.close();
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
    // reset to default option to prevent selecting an unloaded region connector
    this._selectedPermissionAdministrator = null;
    if (this.permissionAdministratorSelectRef.value) {
      this.permissionAdministratorSelectRef.value.selectedIndex = 0;
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

  render() {
    return html`
      <button class="eddie-connect-button" @click="${this.connect}">
        Connect with EDDIE
      </button>

      <dialog class="eddie-connect-dialog" ${ref(this.dialogRef)}>
        <header>
          ${unsafeSVG(eddieLogo)}
        </header>

        <div class="dialog-content">
          ${this._dataNeedAttributes ? html`
            <p>This service is requesting: ${this._dataNeedAttributes.description}</p>
          ` : ""}
          
          <label for="country-select">
            <span>Country</span>
          </label>
          <select id="country-select" @change="${this.handleCountrySelect}">
            <option value="" disabled selected>Select your country</option>
            ${COUNTRIES.map(
              (country) => html`
                <option value="${country}">${COUNTRY_NAMES.of(country)}</option>
              `
            )}
            <optgroup label="Development">
              <option value="sim">Simulation</option>
            </optgroup>
          </select>

          ${this._selectedCountry
            ? html`
                <label for="permission-administrator-select">
                  <span>Permission Administrator</span>
                </label>
                <select
                  ${ref(this.permissionAdministratorSelectRef)}
                  id="permission-administrator-select"
                  @change="${this.handlePermissionAdministratorSelect}"
                >
                  <option value="" disabled selected>
                    Select your Permission Administrator
                  </option>

                  ${PERMISSION_ADMINISTRATORS.filter(
                    (pa) => pa.country === this._selectedCountry
                  ).map(
                    (pa) => html`
                      <option
                        value="${PERMISSION_ADMINISTRATORS.findIndex(
                          (item) => item === pa
                        )}"
                        .disabled="${!this._availableConnectors[
                          pa.regionConnector
                        ]}"
                      >
                        ${pa.company}
                      </option>
                    `
                  )}
                </select>
              `
            : ""}

          <div>
            ${this._selectedPermissionAdministrator
              ? html` ${until(this.getRegionConnectorElement(), "Loading...")} `
              : "No permission administrator selected."}
          </div>
        </div>

        <button class="dialog-close" @click="${this.closePopup}">
          &times;
        </button>
      </dialog>
    `;
  }
}

export default EddieConnectButton;
