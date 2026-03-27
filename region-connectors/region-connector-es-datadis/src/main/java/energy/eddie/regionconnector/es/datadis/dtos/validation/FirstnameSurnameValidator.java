// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos.validation;

import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
public class FirstnameSurnameValidator implements ConstraintValidator<DataNeedCombinationConstraint, PermissionRequestForCreation> {
    private final DataNeedsService dataNeedsService;

    public FirstnameSurnameValidator(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {this.dataNeedsService = dataNeedsService;}

    @Override
    public boolean isValid(PermissionRequestForCreation value, ConstraintValidatorContext context) {
        if (value.dataNeedIds() == null) return true;
        for (var dataNeedId : value.dataNeedIds()) {
            var dn = dataNeedsService.findById(dataNeedId);
            if (dn.isPresent() && dn.get() instanceof CESUJoinRequestDataNeed) {
                if (Strings.isBlank(value.firstname())) {
                    return addViolationFor(context, "firstname");
                }
                if (Strings.isBlank(value.surname())) {
                    return addViolationFor(context, "surname");
                }
                return true;
            }
        }
        return true;
    }

    private boolean addViolationFor(ConstraintValidatorContext context, String fieldName) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("required for CESU Join Request Data Needs")
               .addPropertyNode(fieldName)
               .addConstraintViolation();
        return false;
    }
}
