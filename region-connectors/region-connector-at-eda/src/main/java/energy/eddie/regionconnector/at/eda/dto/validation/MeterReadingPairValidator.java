// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class MeterReadingPairValidator implements ConstraintValidator<MeterReadingPairConstraint, PermissionRequestToImport> {
    @Override
    public boolean isValid(PermissionRequestToImport value, ConstraintValidatorContext context) {
        if (value == null || (value.meterReadingStart() == null) == (value.meterReadingEnd() == null)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
               .addPropertyNode(value.meterReadingStart() == null ? "meterReadingStart" : "meterReadingEnd")
               .addConstraintViolation();
        return false;
    }
}
