package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ObjectFactory;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ParamHistType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
public class ResponseDataTypeBuilderTest {
    @Test
    public void testProcessDirectoryBuilder() {
        // Example of a correct implementation
        ResponseDataTypeBuilder responseDataTypeBuilder = new ResponseDataTypeBuilder();
        responseDataTypeBuilder
                .withConsentId("AT999999201912171011121230023456789")
                .withMeteringPointId("AT9999990699900000000000206868100")
                .withResponseCode(List.of(99))
                .build();
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        ResponseDataTypeBuilder responseDataTypeBuilder = new ResponseDataTypeBuilder();

        // 35 Characters
        assertThrows(IllegalArgumentException.class, () -> responseDataTypeBuilder
                .withConsentId("thisIsARandomStringWithOver35Characters"));
        // 33 Characters
        assertThrows(IllegalArgumentException.class, () -> responseDataTypeBuilder
                .withMeteringPointId("thisIsARandomStringWithOver33Characters"));
    }

    @Test
    public void testEmptyList() {
        // Assign empty list to required attributes
        List<Integer> responseCodes = new ArrayList<>();
        ResponseDataTypeBuilder responseDataTypeBuilder = new ResponseDataTypeBuilder();

        assertThrows(IllegalArgumentException.class, () -> responseDataTypeBuilder
                .withResponseCode(responseCodes));
    }

    @Test
    public void testIllegalStateException() {
        ResponseDataTypeBuilder responseDataTypeBuilder = new ResponseDataTypeBuilder();

        // Assign no attributes
        assertThrows(IllegalStateException.class, responseDataTypeBuilder::build);

        // Assign no required attribute
        assertThrows(IllegalStateException.class, () -> responseDataTypeBuilder
                .withConsentId("Test")
                .withMeteringPointId("Test")
                .withParamHistory(getParamHist())
                .build());

    }

    private List<ParamHistType> getParamHist() {
        ObjectFactory objectFactory = new ObjectFactory();
        List<ParamHistType> paramHist = new ArrayList<>();
        paramHist.add(objectFactory.createParamHistType());

        return paramHist;
    }
}
