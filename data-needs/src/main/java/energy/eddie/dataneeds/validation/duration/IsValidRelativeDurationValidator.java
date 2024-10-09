package energy.eddie.dataneeds.validation.duration;

import energy.eddie.dataneeds.duration.CalendarUnit;
import energy.eddie.dataneeds.duration.RelativeDuration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

public class IsValidRelativeDurationValidator implements ConstraintValidator<IsValidRelativeDuration, RelativeDuration> {
    private final Clock clock;

    public IsValidRelativeDurationValidator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Validates whether the passed {@code duration} is valid. The following table lists valid combinations:
     * <table>
     *     <thead>
     *         <tr>
     *             <th>Start</th>
     *             <th>End</th>
     *             <th>CalendarUnit</th>
     *             <th>Result</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>null</td>
     *             <td>null</td>
     *             <td>null</td>
     *             <td>Open Start; Open End</td>
     *         </tr>
     *         <tr>
     *             <td>null</td>
     *             <td>x</td>
     *             <td>null</td>
     *             <td>Open Start; Fixed End</td>
     *         </tr>
     *         <tr>
     *             <td>x</td>
     *             <td>null</td>
     *             <td>null</td>
     *             <td>Fixed Start; Open End</td>
     *         </tr>
     *         <tr>
     *             <td>x</td>
     *             <td>null</td>
     *             <td>x</td>
     *             <td>Sticky start; Open End</td>
     *         </tr>
     *         <tr>
     *             <td>x</td>
     *             <td>x</td>
     *             <td>null</td>
     *             <td>Fixed Start; Fixed End</td>
     *         </tr>
     *         <tr>
     *             <td>x</td>
     *             <td>x</td>
     *             <td>x</td>
     *             <td>Sticky start; Fixed End</td>
     *         </tr>
     *     </tbody>
     * </table>
     * Note that if both start and end are provided, start must be before or equal to end.
     * As start and end dates are both inclusive, to only fetch data for one day, start may be equal to end.
     * <br>
     * An appropriate error message will be added to the {@code context} explaining why the validation failed.
     *
     * @param duration object to validate
     * @param context  context in which the constraint is evaluated
     * @return True if the duration is valid, false otherwise.
     */
    @Override
    public boolean isValid(RelativeDuration duration, ConstraintValidatorContext context) {
        Optional<Period> start = duration.start();
        Optional<Period> end = duration.end();
        Optional<CalendarUnit> stickyStartCalendarUnit = duration.stickyStartCalendarUnit();


        if (start.isEmpty() && end.isEmpty() && stickyStartCalendarUnit.isPresent()) {
            addCustomViolation("When providing stickyStartCalendarUnit, both start and end have to be supplied as well.",
                               context);
            return false;
        }

        if (start.isEmpty() && end.isPresent() && stickyStartCalendarUnit.isPresent()) {
            addCustomViolation("Open start with stickyStartCalendarUnit is not supported.",
                               context);
            return false;
        }

        if (start.isPresent() && end.isPresent() && isEndBeforeStart(start.get(), end.get())) {
            addCustomViolation("start must be before or equal to end.", context);
            return false;
        }

        // remaining cases are valid:
        // Open Start; Open End
        // Open Start; Fixed End
        // Fixed Start; Open End
        // Sticky start; Open End
        // Fixed Start; Fixed End
        // Sticky start; Fixed End
        return true;
    }

    /**
     * Add a new constraint violation with the passed message to the validation context.
     */
    private void addCustomViolation(String message, ConstraintValidatorContext context) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation()
               .disableDefaultConstraintViolation();
    }

    /**
     * Calculates the relative start and end dates using today as reference and returns whether the end date is before
     * the start date. Today's date is taken from the clock passed as constructor parameter.
     *
     * @param startPeriod Period used to calculate the start date.
     * @param endPeriod   Period used to calculate the end date.
     * @return True if the end date is before the start date, false otherwise.
     */
    private boolean isEndBeforeStart(Period startPeriod, Period endPeriod) {
        LocalDate today = LocalDate.now(clock);

        LocalDate start = today.plus(startPeriod);
        LocalDate end = today.plus(endPeriod);

        return end.isBefore(start);
    }
}
