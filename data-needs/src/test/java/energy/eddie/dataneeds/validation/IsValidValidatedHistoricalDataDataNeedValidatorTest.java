package energy.eddie.dataneeds.validation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsValidValidatedHistoricalDataDataNeedValidatorTest {
    @Mock
    private ConstraintValidatorContext mockContext;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder mockBuilder;
    private final IsValidValidatedHistoricalDataDataNeedValidator validator = new IsValidValidatedHistoricalDataDataNeedValidator();

    public static Stream<Arguments> granularityCombinations() {
        return Stream.of(
                Arguments.of(Granularity.PT5M, Granularity.PT5M, true),
                Arguments.of(Granularity.PT5M, Granularity.PT15M, true),
                Arguments.of(Granularity.PT5M, Granularity.P1Y, true),
                Arguments.of(Granularity.P1Y, Granularity.P1Y, true),
                Arguments.of(Granularity.P1M, Granularity.P1Y, true),
                Arguments.of(Granularity.P1M, Granularity.P1D, false),
                Arguments.of(Granularity.P1Y, Granularity.PT15M, false),
                Arguments.of(Granularity.P1Y, Granularity.PT15M, false)
        );
    }

    @ParameterizedTest
    @DisplayName("Test different granularity combinations")
    @MethodSource("granularityCombinations")
    void givenEndDateBeforeStartDate_validationFails(
            Granularity minGranularity,
            Granularity maxGranularity,
            boolean expectedResult
    ) throws Exception {
        // Given
        if (!expectedResult) {
            when(mockContext.buildConstraintViolationWithTemplate(any())).thenReturn(mockBuilder);
            when(mockBuilder.addConstraintViolation()).thenReturn(mockContext);
        }
        var duration = createVhdDataNeedUsingReflection(minGranularity, maxGranularity);

        // When
        boolean isValid = validator.isValid(duration, mockContext);

        // Then
        assertEquals(expectedResult, isValid);
        // verify expected error message
        if (!isValid) {
            verify(mockContext).buildConstraintViolationWithTemplate(
                    "maxGranularity must be higher or equal to minGranularity.");
        }
    }

    private ValidatedHistoricalDataDataNeed createVhdDataNeedUsingReflection(
            Granularity minGranularity,
            Granularity maxGranularity
    ) throws Exception {
        Constructor<?> constructor = Class.forName(ValidatedHistoricalDataDataNeed.class.getName())
                                          .getDeclaredConstructor();
        constructor.setAccessible(true);

        ValidatedHistoricalDataDataNeed dataNeed = (ValidatedHistoricalDataDataNeed) constructor.newInstance();

        Field minField = dataNeed.getClass().getDeclaredField("minGranularity");
        Field maxField = dataNeed.getClass().getDeclaredField("maxGranularity");
        minField.setAccessible(true);
        maxField.setAccessible(true);

        minField.set(dataNeed, minGranularity);
        maxField.set(dataNeed, maxGranularity);
        return dataNeed;
    }
}
