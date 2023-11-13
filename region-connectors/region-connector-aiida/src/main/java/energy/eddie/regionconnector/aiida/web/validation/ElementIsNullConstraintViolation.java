package energy.eddie.regionconnector.aiida.web.validation;

import jakarta.validation.ConstraintValidatorContext;

class ElementIsNullConstraintViolation {
    private final ConstraintValidatorContext context;

    ElementIsNullConstraintViolation(ConstraintValidatorContext context) {
        this.context = context;
    }

    boolean elementIsNull(String messageTemplate) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addConstraintViolation();
        return false;
    }
}
