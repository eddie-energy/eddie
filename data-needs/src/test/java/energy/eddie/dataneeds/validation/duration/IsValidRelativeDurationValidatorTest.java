package energy.eddie.dataneeds.validation.duration;

import energy.eddie.dataneeds.duration.CalendarUnit;
import energy.eddie.dataneeds.duration.RelativeDuration;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Period;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsValidRelativeDurationValidatorTest {
    @Mock
    private ConstraintValidatorContext mockContext;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder mockBuilder;
    private final IsValidRelativeDurationValidator validator = new IsValidRelativeDurationValidator(Clock.systemUTC());

    public static Stream<Arguments> relativeDurationArgs() {
        var start = Period.parse("-P1Y");
        var end = Period.parse("+P1Y");

        return Stream.of(
                Arguments.of(null, null, null, true),
                Arguments.of(null, null, CalendarUnit.MONTH, false),
                Arguments.of(null, end, null, true),
                Arguments.of(null, end, CalendarUnit.WEEK, false),
                Arguments.of(start, null, null, true),
                Arguments.of(start, null, CalendarUnit.YEAR, true),
                Arguments.of(end, start, null, false),
                Arguments.of(start, end, null, true),
                Arguments.of(end, start, CalendarUnit.MONTH, false),
                Arguments.of(start, end, CalendarUnit.MONTH, true),
                Arguments.of(start, start, CalendarUnit.MONTH, true),
                Arguments.of(end, end, null, true)
        );
    }

    @ParameterizedTest
    @DisplayName("Check all different combinations for relative duration")
    @MethodSource("relativeDurationArgs")
    void givenRelativeDuration_validates(
            Period start,
            Period end,
            CalendarUnit stickyStartCalendarUnit,
            boolean expectedResult
    ) throws Exception {
        // mocks only needed for failed validations
        if (!expectedResult) {
            when(mockContext.buildConstraintViolationWithTemplate(any())).thenReturn(mockBuilder);
            when(mockBuilder.addConstraintViolation()).thenReturn(mockContext);
        }

        // Given
        var duration = createDurationUsingReflection(start, end, stickyStartCalendarUnit);

        // When
        boolean isValid = validator.isValid(duration, mockContext);

        // Then
        assertEquals(expectedResult, isValid);
    }

    /**
     * Uses reflections to avoid having to create a constructor or setters which are just used by the tests.
     */
    private RelativeDuration createDurationUsingReflection(
            Period start,
            Period end,
            CalendarUnit stickyStartCalendarUnit
    ) throws Exception {
        Constructor<?> constructor = Class.forName(RelativeDuration.class.getName()).getDeclaredConstructor();
        constructor.setAccessible(true);

        RelativeDuration duration = (RelativeDuration) constructor.newInstance();

        Field startField = duration.getClass().getDeclaredField("start");
        Field endField = duration.getClass().getDeclaredField("end");
        Field stickyStartCalendarUnitField = duration.getClass().getDeclaredField("stickyStartCalendarUnit");
        startField.setAccessible(true);
        endField.setAccessible(true);
        stickyStartCalendarUnitField.setAccessible(true);

        startField.set(duration, start);
        endField.set(duration, end);
        stickyStartCalendarUnitField.set(duration, stickyStartCalendarUnit);
        return duration;
    }
}
