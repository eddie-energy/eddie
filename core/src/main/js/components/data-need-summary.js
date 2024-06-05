import { html, TemplateResult } from "lit";
import { relativeDateFromDuration } from "../duration.js";

import { DATA_NEED_TOOLTIPS } from "../constants/data-need-tooltips.js";
import { ENERGY_TYPES } from "../constants/energy-types.js";
import { GRANULARITIES } from "../constants/granularities.js";

/**
 * @param {DataNeedAttributes} dataNeedAttributes
 * @returns {TemplateResult<1>}
 */
export function dataNeedSummary({
  type,
  purpose,
  policyLink,
  duration,
  minGranularity,
  maxGranularity,
  energyType,
  transmissionInterval,
  dataTags,
}) {
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
        ${duration && durationDescription(duration)}

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
              <dd>${ENERGY_TYPES[energyType] ?? energyType}</dd>
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

/**
 * @param {AbsoluteDuration|RelativeDuration} duration
 * @return {TemplateResult<1>}
 */
function durationDescription(duration) {
  const [start, end] = datesFromDuration(duration);
  return html`
    <dt>Duration</dt>
    <dd>From ${start.toLocaleDateString()} to ${end.toLocaleDateString()}</dd>
  `;
}

/**
 *
 * @param {AbsoluteDuration|RelativeDuration} duration
 * @return {[Date, Date]}
 */
function datesFromDuration(duration) {
  if (duration.type === "relativeDuration") {
    return [
      relativeDateFromDuration(
        duration.start,
        duration.stickyStartCalendarUnit
      ),
      relativeDateFromDuration(duration.end, duration.stickyStartCalendarUnit),
    ];
  }

  return [new Date(duration.start), new Date(duration.end)];
}
