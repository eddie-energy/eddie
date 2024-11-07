import { DATA_NEED_TOOLTIPS } from "./constants/data-need-tooltips.js";
import { GRANULARITIES } from "./constants/granularities.js";
import { relativeDateFromDuration } from "./duration.js";
import { ENERGY_TYPES } from "./constants/energy-types.js";

const CORE_URL =
  import.meta.env.VITE_CORE_URL ?? new URL(import.meta.url).origin;

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

    const response = await fetch(`${CORE_URL}/data-needs/api/${dataNeedId}`);

    if (!response.ok) {
      return;
    }

    const dataNeed = await response.json();

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
      </style>
      <sl-alert open>
        <dl>
          <dt>Type of data</dt>
          <dd>
            ${title}
            <sl-tooltip content="${details}">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                class="bi bi-info-circle"
                viewBox="0 0 16 16"
                style="display: inline-block; width: 1em; height: 1em; transform: translateY(1px)"
              >
                <path
                  d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"
                ></path>
                <path
                  d="m8.93 6.588-2.29.287-.082.38.45.083c.294.07.352.176.288.469l-.738 3.468c-.194.897.105 1.319.808 1.319.545 0 1.178-.252 1.465-.598l.088-.416c-.2.176-.492.246-.686.246-.275 0-.375-.193-.304-.533L8.93 6.588zM9 4.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0z"
                ></path>
              </svg>
            </sl-tooltip>
          </dd>

          <!-- For timeframed data needs -->
          ${duration ? this.durationDescription(duration) : ""}

          <!-- For validated historical data data needs -->
          ${minGranularity
            ? this.granularityDescription(minGranularity, maxGranularity)
            : ""}
          ${energyType
            ? `<dt>Energy Type</dt><dd>${ENERGY_TYPES[energyType] || energyType}</dd>`
            : ""}

          <!-- For AIIDA data needs -->
          ${transmissionSchedule
            ? `<dt>Transmission Schedule</dt><dd>${transmissionSchedule}</dd>`
            : ""}

          <!-- For AIIDA schemas -->
          ${schemas
            ? `<dt>Schemas:</dt><dd>${schemas.join(", ")}</dd>`
            : ""}
          
          <!-- For AIIDA smart meter data needs -->
          ${dataTags
            ? `<dt>OBIS Points</dt><dd>${dataTags.join(", ")}</dd>`
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

  granularityDescription(minGranularity, maxGranularity) {
    return /* HTML */ `
      <dt>Granularity</dt>
      <dd>
        ${minGranularity === maxGranularity
          ? GRANULARITIES[minGranularity]
          : `${GRANULARITIES[minGranularity]} if possible. At least ${GRANULARITIES[maxGranularity].toLowerCase()}.`}
      </dd>
    `;
  }

  durationDescription(duration) {
    const [start, end] = this.datesFromDuration(duration);
    return `
      <dt>Duration</dt>
      <dd>From ${start.toLocaleDateString()} to ${end.toLocaleDateString()}</dd>
    `;
  }

  datesFromDuration(duration) {
    if (duration.type === "relativeDuration") {
      return [
        relativeDateFromDuration(
          duration.start,
          duration.stickyStartCalendarUnit
        ),
        relativeDateFromDuration(
          duration.end,
          duration.stickyStartCalendarUnit
        ),
      ];
    }
    return [new Date(duration.start), new Date(duration.end)];
  }
}

customElements.define("data-need-summary", DataNeedSummary);
