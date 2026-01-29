// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.validation.duration;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsValidAbsoluteDurationValidator implements ConstraintValidator<IsValidAbsoluteDuration, AbsoluteDuration> {
    /**
     * Validates whether the passed {@code duration} is valid. An absolute duration is valid if the start date is before
     * or equal to the end date.
     *
     * @param duration object to validate
     * @param context  context in which the constraint is evaluated
     * @return True if the duration is valid, false otherwise.
     */
    @Override
    public boolean isValid(AbsoluteDuration duration, ConstraintValidatorContext context) {
        if (duration.end().isBefore(duration.start())) {
            context.buildConstraintViolationWithTemplate("start must be before or equal to end.")
                   .addConstraintViolation()
                   .disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }
}
