package energy.eddie.dataneeds.validation.duration;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsValidAbsoluteDurationValidatorTest {
    @Mock
    private ConstraintValidatorContext mockContext;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder mockBuilder;
    private final IsValidAbsoluteDurationValidator validator = new IsValidAbsoluteDurationValidator();

    @Test
    void givenEndDateBeforeStartDate_validationFails() throws Exception {
        // Given
        when(mockContext.buildConstraintViolationWithTemplate(any())).thenReturn(mockBuilder);
        when(mockBuilder.addConstraintViolation()).thenReturn(mockContext);
        var duration = createDurationUsingReflection(LocalDate.of(2024, 4, 5),
                                                     LocalDate.of(2024, 4, 1));

        // When
        boolean isValid = validator.isValid(duration, mockContext);

        // Then
        assertFalse(isValid);
    }

    @Test
    void givenEndIsEqualToStartDate_validationPasses() throws Exception {
        // Given
        var duration = createDurationUsingReflection(LocalDate.of(2024, 4, 1),
                                                     LocalDate.of(2024, 4, 1));

        // When
        boolean isValid = validator.isValid(duration, mockContext);

        // Then
        assertTrue(isValid);
    }

    @Test
    void givenEndIsAfterStartDate_validationPasses() throws Exception {
        // Given
        var duration = createDurationUsingReflection(LocalDate.of(2024, 4, 1),
                                                     LocalDate.of(2024, 4, 21));

        // When
        boolean isValid = validator.isValid(duration, mockContext);

        // Then
        assertTrue(isValid);
    }

    /**
     * Uses reflections to avoid having to create a constructor or setters which are just used by the tests.
     */
    private AbsoluteDuration createDurationUsingReflection(
            LocalDate start,
            LocalDate end
    ) throws Exception {
        Constructor<?> constructor = Class.forName(AbsoluteDuration.class.getName()).getDeclaredConstructor();
        constructor.setAccessible(true);

        AbsoluteDuration duration = (AbsoluteDuration) constructor.newInstance();

        Field startField = duration.getClass().getDeclaredField("start");
        Field endField = duration.getClass().getDeclaredField("end");
        startField.setAccessible(true);
        endField.setAccessible(true);

        startField.set(duration, start);
        endField.set(duration, end);
        return duration;
    }
}
