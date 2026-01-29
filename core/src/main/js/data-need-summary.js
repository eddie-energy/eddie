// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/alert/alert.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/icon/icon.js";
import "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.15.0/cdn/components/tooltip/tooltip.js";

import cronstrue from "cronstrue";

import { getDataNeedAttributes } from "./api.js";
import { datesFromDuration } from "./data-need-util.js";

import { GRANULARITIES } from "./constants/granularities.js";
import { ENERGY_TYPES } from "./constants/energy-types.js";
import { DATA_NEED_TOOLTIPS } from "./constants/data-need-tooltips.js";

class DataNeedSummary extends HTMLElement {
  static get observedAttributes() {
    return ["data-need-id"];
  }

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  async render() {
    const dataNeedId = this.getAttribute("data-need-id");
    const dataNeed = await getDataNeedAttributes(dataNeedId);

    const {
      type,
      purpose,
      policyLink,
      duration,
      minGranularity,
      maxGranularity,
      energyType,
      transmissionSchedule,
      schemas,
      asset,
      dataTags,
    } = dataNeed;

    const [title, details] = DATA_NEED_TOOLTIPS[type];

    this.shadowRoot.innerHTML = /* HTML */ `
      <style>
        dl {
          display: grid;
          grid-template-columns: auto 1fr;
        }
        dt {
          font-weight: bold;
        }
        dd::first-letter {
          text-transform: capitalize;
        }
      </style>

      <sl-alert open>
        <dl>
          <dt>Type of data</dt>
          <dd>
            <sl-tooltip content="${details}">
              ${title}
              <sl-icon name="info-circle"></sl-icon>
            </sl-tooltip>
          </dd>

          <!-- For timeframed data needs -->
          ${duration
            ? /* HTML */ `
                <dt>Duration</dt>
                <dd>${this.durationDescription(duration)}</dd>
              `
            : ""}

          <!-- For validated historical data data needs -->
          ${minGranularity
            ? /* HTML */ `
                <dt>Granularity</dt>
                <dd>
                  ${minGranularity === maxGranularity
                    ? GRANULARITIES[minGranularity]
                    : `${GRANULARITIES[minGranularity]} if possible. At least ${GRANULARITIES[maxGranularity]}.`}
                </dd>
              `
            : ""}
          ${energyType
            ? /* HTML */ `
                <dt>Energy Type</dt>
                <dd>${ENERGY_TYPES[energyType] || energyType}</dd>
              `
            : ""}

          <!-- For AIIDA data needs -->
          ${transmissionSchedule
            ? /* HTML */ `
                <dt>Transmission Schedule</dt>
                <dd>${cronstrue.toString(transmissionSchedule)}</dd>
              `
            : ""}

          <!-- For AIIDA data needs -->
          ${schemas
            ? /* HTML */ `
                <dt>Schemas:</dt>
                <dd>${schemas.join(", ")}</dd>
              `
            : ""}

          <!-- For AIIDA data needs -->
          ${transmissionSchedule
            ? /* HTML */ `
                <dt>Asset</dt>
                <dd>${asset}</dd>
              `
            : ""}

          <!-- For AIIDA smart meter data needs -->
          ${dataTags
            ? /* HTML */ `
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
          <a href="${policyLink}" target="_blank">Data Usage Policy</a> of the
          service provider.
        </p>
      </sl-alert>
    `;
  }

  durationDescription(duration) {
    const [start, end] = datesFromDuration(duration);
    return `From ${start.toLocaleDateString()} to ${end.toLocaleDateString()}`;
  }
}

customElements.define("data-need-summary", DataNeedSummary);
