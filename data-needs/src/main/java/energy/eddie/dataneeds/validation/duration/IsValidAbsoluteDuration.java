// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.validation.duration;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsValidAbsoluteDurationValidator.class)
public @interface IsValidAbsoluteDuration {
    String message() default "Validation for absolute duration failed.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
