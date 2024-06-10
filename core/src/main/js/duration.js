import { parse } from "tinyduration";

/**
 * Returns a date that is the current date plus the duration.
 * @param {string} durationString ISO 8601 duration string to add to the current date
 * @param {CalendarUnit} [stickyStartCalendarUnit]
 * @returns {Date} Date after applying the duration to the current date
 */
export function relativeDateFromDuration(durationString, stickyStartCalendarUnit = undefined) {
  const duration = parse(durationString);
  const now = new Date();

  duration.years && now.setFullYear(now.getFullYear() + duration.years);
  duration.months && now.setMonth(now.getMonth() + duration.months);
  duration.days && now.setDate(now.getDate() + duration.days);
  duration.hours && now.setHours(now.getHours() + duration.hours);
  duration.minutes && now.setMinutes(now.getMinutes() + duration.minutes);
  duration.seconds && now.setSeconds(now.getSeconds() + duration.seconds);

  switch (stickyStartCalendarUnit) {
    case "WEEK":
      now.setDate(now.getDate() - now.getDay());
      break;
    case "MONTH":
      now.setDate(1);
      break;
    case "YEAR":
      now.setMonth(0, 1);
      break;
  }

  return now;
}
