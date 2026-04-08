// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class CESUJoinRequestValidator implements ConstraintValidator<DataNeedCombinationConstraint, PermissionRequestForCreation> {
    public static final String MSG_TEMPLATE = "%s is required for CESU Join Request Data Needs";
    private final DataNeedsService dataNeedsService;

    public CESUJoinRequestValidator(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {this.dataNeedsService = dataNeedsService;}

    @Override
    public boolean isValid(PermissionRequestForCreation value, ConstraintValidatorContext context) {
        if (value.dataNeedIds() == null) return true;
        for (var dataNeedId : value.dataNeedIds()) {
            var dn = dataNeedsService.findById(dataNeedId);
            if (dn.isEmpty() || !(dn.get() instanceof CESUJoinRequestDataNeed dataNeed)) {
                continue;
            }
            if (value.meteringPointId() == null) {
                return addViolationFor(context, "meteringPointId");
            }
            if (dataNeed.energyDirection().orElseGet(value::energyDirection) == null) {
                return addViolationFor(context, "energyDirection");
            }
            if (dataNeed.participationFactor().orElseGet(value::participationFactor) == null) {
                return addViolationFor(context, "participationFactor");
            }
        }
        return true;
    }

    private static boolean addViolationFor(ConstraintValidatorContext context, String participationFactor) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(MSG_TEMPLATE.formatted(participationFactor))
               .addConstraintViolation();
        return false;
    }
}
