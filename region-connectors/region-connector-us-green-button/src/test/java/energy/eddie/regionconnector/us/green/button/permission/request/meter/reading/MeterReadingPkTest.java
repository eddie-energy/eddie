package energy.eddie.regionconnector.us.green.button.permission.request.meter.reading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MeterReadingPkTest {
    static Stream<Arguments> testEqualsAndHashCode_DifferentPermissionId() {
        return Stream.of(
                Arguments.of(
                        new MeterReadingPk("permission123", "meter456"),
                        new MeterReadingPk("permission789", "meter456")
                ),
                Arguments.of(
                        new MeterReadingPk("permission123", "meter456"),
                        new MeterReadingPk("permission123", "meter789")
                ),
                Arguments.of(
                        new MeterReadingPk("permission123", "meter456"),
                        new MeterReadingPk("permission789", "meter999")
                )
        );
    }

    @Test
    void testEqualsAndHashCode_EqualObjects() {
        // Given
        MeterReadingPk pk1 = new MeterReadingPk("permission123", "meter456");
        MeterReadingPk pk2 = new MeterReadingPk("permission123", "meter456");

        // When
        boolean equals = pk1.equals(pk2);
        int hashCode1 = pk1.hashCode();
        int hashCode2 = pk2.hashCode();

        // Then
        assertTrue(equals);
        assertEquals(hashCode1, hashCode2);
    }

    @ParameterizedTest
    @MethodSource
    void testEqualsAndHashCode_DifferentPermissionId(MeterReadingPk pk1, MeterReadingPk pk2) {
        // Given

        // When
        boolean equals = pk1.equals(pk2);
        int hashCode1 = pk1.hashCode();
        int hashCode2 = pk2.hashCode();

        // Then
        assertFalse(equals);
        assertNotEquals(hashCode1, hashCode2);
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void testEqualsWithNull() {
        // Given
        MeterReadingPk pk = new MeterReadingPk("permission123", "meter456");

        // When
        boolean equals = pk.equals(null);

        // Then
        assertFalse(equals);
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Test
    void testEqualsWithDifferentType() {
        // Given
        MeterReadingPk pk = new MeterReadingPk("permission123", "meter456");
        String notAPk = "not a MeterReadingPk";

        // When
        boolean equals = pk.equals(notAPk);

        // Then
        assertFalse(equals);
    }

    @Test
    void testEqualsAndHashCode_DifferentObjects() {
        // Given
        MeterReadingPk pk1 = new MeterReadingPk("permission123", "meter456");
        MeterReadingPk pk2 = new MeterReadingPk("permission789", "meter999");

        // When
        boolean equals = pk1.equals(pk2);
        int hashCode1 = pk1.hashCode();
        int hashCode2 = pk2.hashCode();

        // Then
        assertFalse(equals);
        assertNotEquals(hashCode1, hashCode2);
    }
}