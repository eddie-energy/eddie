package energy.eddie.regionconnector.dk.energinet.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointQualityEnumTest {
    @Test
    void fromString_asExpected() {
        //given
        //when
        //then
        assertEquals(PointQualityEnum.A01, PointQualityEnum.fromString("A01"));
        assertEquals(PointQualityEnum.A02, PointQualityEnum.fromString("A02"));
        assertEquals(PointQualityEnum.A03, PointQualityEnum.fromString("A03"));
        assertEquals(PointQualityEnum.A04, PointQualityEnum.fromString("A04"));
        assertEquals(PointQualityEnum.A05, PointQualityEnum.fromString("A05"));
    }

    @Test
    void fromString_invalidCode_throws() {
        //given
        //when
        //then
        assertThrows(IllegalArgumentException.class, () -> PointQualityEnum.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> PointQualityEnum.fromString("A06"));
    }

    @Test
    void fromString_codeIsNull_throws() {
        //given
        //when
        //then
        assertThrows(NullPointerException.class, () -> PointQualityEnum.fromString(null));
    }
}
