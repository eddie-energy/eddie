package energy.eddie.regionconnector.at.eda.requests.restricted.enums;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllowedMeteringIntervalTypeTest {
    @Test
    void allowedMeteringIntervalTypeValue() {
        // given
        AllowedMeteringIntervalType type = AllowedMeteringIntervalType.QH;
        MeteringIntervallType expectedValue = MeteringIntervallType.QH;

        // when
        MeteringIntervallType actualValue = type.value();

        // then
        assertEquals(expectedValue, actualValue);
    }


    @Test
    void allowedMeteringIntervalTypeValueOfValidInput() {
        // given
        String input = "QH";
        AllowedMeteringIntervalType expectedValue = AllowedMeteringIntervalType.QH;

        // when
        AllowedMeteringIntervalType actualValue = AllowedMeteringIntervalType.valueOf(input);

        // then
        assertEquals(expectedValue, actualValue);
    }


}