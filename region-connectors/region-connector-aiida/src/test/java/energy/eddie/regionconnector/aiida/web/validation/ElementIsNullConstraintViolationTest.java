package energy.eddie.regionconnector.aiida.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElementIsNullConstraintViolationTest {
    @Test
    void elementIsNull_ShouldBuildConstraintViolation() {
        // Given
        String messageTemplate = "Custom error message";
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        when(context.buildConstraintViolationWithTemplate(any()))
                .thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));
        ElementIsNullConstraintViolation validator = new ElementIsNullConstraintViolation(context);

        // When
        boolean result = validator.elementIsNull(messageTemplate);

        // Then
        Mockito.verify(context).disableDefaultConstraintViolation();
        Mockito.verify(context).buildConstraintViolationWithTemplate(messageTemplate);
        assertFalse(result);
    }
}