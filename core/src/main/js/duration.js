import { parse } from "tinyduration";

/**
 * Returns a date that is the current date plus the duration.
 * @param {string} durationString ISO 8601 duration string to add to the current date
 * @returns {Date} Date after applying the duration to the current date
 */
export function relativeDateFromDuration(durationString) {
  const duration = parse(durationString);
  const now = new Date();

  duration.years && now.setFullYear(now.getFullYear() + duration.years);
  duration.months && now.setMonth(now.getMonth() + duration.months);
  duration.days && now.setDate(now.getDate() + duration.days);
  duration.hours && now.setHours(now.getHours() + duration.hours);
  duration.minutes && now.setMinutes(now.getMinutes() + duration.minutes);
  duration.seconds && now.setSeconds(now.getSeconds() + duration.seconds);

  return now;
}
