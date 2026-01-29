// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { relativeDateFromDuration } from "./duration.js";

import { ENERGY_TYPES } from "./constants/energy-types.js";
import { GRANULARITIES } from "./constants/granularities.js";

import cronstrue from "cronstrue";
import { DATA_NEED_TOOLTIPS } from "./constants/data-need-tooltips.js";

/**
 * Generates a short description of the data need with relevant attributes.
 * > Validated Historical Data 🛈 for Electricity from 8/25/2024 to 11/24/2024 in an interval of 15 minutes to 1 day
 * @param {DataNeedAttributes} dataNeed - The attributes of the data need.
 * @returns {string} - The summary of the data need.
 */
export function dataNeedDescription(dataNeed) {
  const {
    type,
    duration,
    minGranularity,
    maxGranularity,
    energyType,
    transmissionSchedule,
    dataTags,
  } = dataNeed;

  const [title, details] = DATA_NEED_TOOLTIPS[type];

  let summary = `
    <sl-tooltip content="${details}"><i>${title}</i> 🛈</sl-tooltip>
  `;

  if (energyType) {
    summary += ` for <i>${ENERGY_TYPES[energyType] || energyType}</i>`;
  }

  if (duration) {
    const [start, end] = datesFromDuration(duration);
    summary += ` from <i>${start.toLocaleDateString()}</i> to <i>${end.toLocaleDateString()}</i>`;
  }

  if (minGranularity) {
    summary += ` in an interval of <i>${GRANULARITIES[minGranularity]}</i>`;

    if (maxGranularity) {
      summary += ` to <i>${GRANULARITIES[maxGranularity]}</i>`;
    }
  }

  if (transmissionSchedule) {
    summary += ` <i>${cronstrue.toString(transmissionSchedule).toLocaleLowerCase()}</i>`;
  }

  if (dataTags) {
    summary += ` for the following OBIS codes: <i>${dataTags.join(", ")}</i>`;
  }

  return summary;
}

/**
 * Returns the start and end date for the given data need duration.
 * @param {AbsoluteDuration|RelativeDuration} duration - The duration of the data need.
 * @returns {[Date, Date]} - The start and end date of the duration.
 */
export function datesFromDuration(duration) {
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
