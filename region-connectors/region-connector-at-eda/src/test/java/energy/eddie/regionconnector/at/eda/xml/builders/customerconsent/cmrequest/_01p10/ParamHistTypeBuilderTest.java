package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParamHistTypeBuilderTest {
    @Test
    void testParamCycTypeBuilder() {
        // Example of a correct implementation
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder()
                .withMeteringIntervall(MeteringIntervallType.D);

        assertDoesNotThrow(paramHistTypeBuilder::build);
    }

    @Test
    void testNullPointerException() {
        // Assign no attributes
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder();
        assertThrows(NullPointerException.class, paramHistTypeBuilder::build);
    }
}
