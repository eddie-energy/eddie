package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ObjectFactory;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReqDatParamTypeBuilderTest {
    @Test
    public void testReqDatParamTypeBuilder() {
        // Example of a correct implementation
        // Assign either paramCyc OR paramHist
        ReqDatParamTypeBuilder reqDatParamTypeBuilderWithCyc = new ReqDatParamTypeBuilder();
        ReqDatParamTypeBuilder reqDatParamTypeBuilderWithHist = new ReqDatParamTypeBuilder();
        ParamCycTypeBuilder paramCycTypeBuilder = new ParamCycTypeBuilder();
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder();

        reqDatParamTypeBuilderWithHist
                .withParamHist(
                        paramHistTypeBuilder
                                .withMeteringIntervall(MeteringIntervallType.D)
                                .build()
                ).build();
        reqDatParamTypeBuilderWithCyc
                .withParamCyc(
                        paramCycTypeBuilder
                                .withMeteringIntervall(MeteringIntervallType.QH)
                                .withTransmissionCycle(TransmissionCycle.M)
                                .build()
                ).build();
    }

    @Test
    public void testIllegalStateException() {
        // Assign no attributes
        ReqDatParamTypeBuilder reqDatParamTypeBuilder = new ReqDatParamTypeBuilder();
        assertThrows(IllegalStateException.class, reqDatParamTypeBuilder::build);

        // Assign paramCyc AND paramHist
        ObjectFactory objectFactory = new ObjectFactory();
        assertThrows(IllegalStateException.class, () -> reqDatParamTypeBuilder
                .withParamCyc(objectFactory.createParamCycType())
                .withParamHist(objectFactory.createParamHistType())
                .build());
    }
}
