package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ObjectFactory;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReqDatParamTypeBuilderTest {
    @Test
    void testReqDatParamTypeBuilder() {
        // Example of a correct implementation
        // Assign either paramCyc OR paramHist
        ReqDatParamTypeBuilder reqDatParamTypeBuilderWithCyc = new ReqDatParamTypeBuilder();
        ReqDatParamTypeBuilder reqDatParamTypeBuilderWithHist = new ReqDatParamTypeBuilder();
        ParamCycTypeBuilder paramCycTypeBuilder = new ParamCycTypeBuilder();
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder();

        assertDoesNotThrow(reqDatParamTypeBuilderWithHist
                .withParamHist(
                        paramHistTypeBuilder
                                .withMeteringIntervall(MeteringIntervallType.D)
                                .build()
                )::build);
        assertDoesNotThrow(reqDatParamTypeBuilderWithCyc
                .withParamCyc(
                        paramCycTypeBuilder
                                .withMeteringIntervall(MeteringIntervallType.QH)
                                .withTransmissionCycle(TransmissionCycle.M)
                                .build()
                )::build);
    }

    @Test
    void testNullPointerException() {
        // Assign no attributes
        ReqDatParamTypeBuilder reqDatParamTypeBuilder = new ReqDatParamTypeBuilder();
        assertThrows(NullPointerException.class, reqDatParamTypeBuilder::build);

        // Assign paramCyc AND paramHist
        ObjectFactory objectFactory = new ObjectFactory();
        assertThrows(NullPointerException.class, reqDatParamTypeBuilder
                .withParamCyc(objectFactory.createParamCycType())
                .withParamHist(objectFactory.createParamHistType())
                ::build);
    }
}
