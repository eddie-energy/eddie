package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParamHistTypeBuilderTest {
    @Test
    public void testParamCycTypeBuilder() {
        // Example of a correct implementation
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder();
        paramHistTypeBuilder
                .withMeteringIntervall(MeteringIntervallType.D)
                .build();
    }

    @Test
    public void testIllegalStateException() {
        // Assign no attributes
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder();
        assertThrows(IllegalStateException.class, paramHistTypeBuilder::build);
    }
}
