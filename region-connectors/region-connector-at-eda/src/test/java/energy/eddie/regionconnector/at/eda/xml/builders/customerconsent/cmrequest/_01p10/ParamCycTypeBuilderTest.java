package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParamCycTypeBuilderTest {
    @Test
    void testParamCycTypeBuilder() {
        // Example of a correct implementation
        ParamCycTypeBuilder paramCycTypeBuilder = new ParamCycTypeBuilder()
                .withMeteringIntervall(MeteringIntervallType.QH)
                .withTransmissionCycle(TransmissionCycle.M);
        assertDoesNotThrow(paramCycTypeBuilder::build);
    }

    @Test
    void testNullPointerException() {
        // Assign no attributes
        ParamCycTypeBuilder paramCycTypeBuilder1 = new ParamCycTypeBuilder();
        ParamCycTypeBuilder paramCycTypeBuilder2 = new ParamCycTypeBuilder();
        assertThrows(NullPointerException.class, paramCycTypeBuilder1::build);

        // Only one attribute assigned
        assertThrows(NullPointerException.class, paramCycTypeBuilder1.withMeteringIntervall(MeteringIntervallType.QH)::build);
        assertThrows(NullPointerException.class, paramCycTypeBuilder2.withTransmissionCycle(TransmissionCycle.M)::build);
    }
}
