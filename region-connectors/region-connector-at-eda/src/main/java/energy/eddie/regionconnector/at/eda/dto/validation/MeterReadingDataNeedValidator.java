// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import energy.eddie.api.agnostic.data.needs.CESUJoinRequestDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class MeterReadingDataNeedValidator implements ConstraintValidator<MeterReadingDataNeedConstraint, PermissionRequestToImport> {
    private final DataNeedCalculationService calculationService;

    public MeterReadingDataNeedValidator(DataNeedCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @Override
    public boolean isValid(PermissionRequestToImport value, ConstraintValidatorContext context) {
        if (value == null || value.dataNeedId() == null || value.creationDateTime() == null
            || value.meterReadingStart() == null || value.meterReadingEnd() == null) {
            return true;
        }

        var result = calculationService.calculate(value.dataNeedId(), value.creationDateTime());
        return switch (result) {
            case ValidatedHistoricalDataDataNeedResult historical ->
                    validateTimeframe(historical.energyTimeframe(), value, context);
            case CESUJoinRequestDataNeedResult cesu -> validateTimeframe(cesu.permissionTimeframe(), value, context);
            case null, default -> true;
        };
    }

    @SuppressWarnings({"DataFlowIssue", "NullAway"})
    private boolean validateTimeframe(
            Timeframe timeframe,
            PermissionRequestToImport value,
            ConstraintValidatorContext context
    ) {
        var valid = true;
        context.disableDefaultConstraintViolation();
        if (timeframe.start().isAfter(value.meterReadingStart().toLocalDate())) {
            context.buildConstraintViolationWithTemplate(
                           "Requested meter reading start date is after the calculated timeframe")
                   .addPropertyNode("meterReadingStart")
                   .addConstraintViolation();
            valid = false;
        }
        if (timeframe.end().isBefore(value.meterReadingEnd().toLocalDate())) {
            context.buildConstraintViolationWithTemplate(
                           "Requested meter reading end date is before the calculated timeframe")
                   .addPropertyNode("meterReadingEnd")
                   .addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
