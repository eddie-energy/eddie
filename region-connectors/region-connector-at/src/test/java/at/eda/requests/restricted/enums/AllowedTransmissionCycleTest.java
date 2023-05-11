package at.eda.requests.restricted.enums;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllowedTransmissionCycleTest {
    @Test
    void allowedTransmissionCycleValue() {
        // given
        AllowedTransmissionCycle transmissionCycle = AllowedTransmissionCycle.D;
        TransmissionCycle expectedValue = TransmissionCycle.D;

        // when
        TransmissionCycle actualValue = transmissionCycle.value();

        // then
        assertEquals(expectedValue, actualValue);
    }
}