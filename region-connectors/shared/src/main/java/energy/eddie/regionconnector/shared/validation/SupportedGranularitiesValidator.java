package energy.eddie.regionconnector.shared.validation;

import energy.eddie.api.agnostic.Granularity;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SupportedGranularitiesValidator implements ConstraintValidator<SupportedGranularities, Granularity> {

    private Set<Granularity> allowedValues;
    private String allowedValuesString;

    @Override
    public void initialize(SupportedGranularities constraintAnnotation) {
        allowedValues = new HashSet<>(Arrays.asList(constraintAnnotation.value()));
        allowedValuesString = allowedValues.stream()
                .map(Granularity::name)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isValid(Granularity value, ConstraintValidatorContext context) {
        if (value == null || allowedValues.contains(value)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Unsupported granularity: '%s'. Supported granularities are: %s".formatted(value, allowedValuesString))
                .addConstraintViolation();

        return false;
    }
}