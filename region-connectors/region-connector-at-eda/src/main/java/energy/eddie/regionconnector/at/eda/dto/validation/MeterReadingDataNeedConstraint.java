// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MeterReadingDataNeedValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MeterReadingDataNeedConstraint {
    String message() default "Requested meter reading dates are outside the calculated data need timeframe";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
