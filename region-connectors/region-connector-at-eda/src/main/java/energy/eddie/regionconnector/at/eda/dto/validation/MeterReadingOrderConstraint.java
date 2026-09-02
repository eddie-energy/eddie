// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MeterReadingOrderValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MeterReadingOrderConstraint {
    String message() default "Requested meter reading start date is after the meter reading end date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
