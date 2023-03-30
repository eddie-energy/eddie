package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ResponseDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessDirectoryBuilderTest {
    @Test
    public void testProcessDirectoryBuilder() {
        // Example of a correct implementation
        ResponseDataTypeBuilder responseDataTypeBuilder = new ResponseDataTypeBuilder();
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        processDirBuilder
                .withMessageId("GC100007201912170930001230001234567")
                .withConversationId("GC100007201912170930001230012345678")
                .withCMRequestId("IWRN74PW")
                .withResponseData(List.of(
                        responseDataTypeBuilder
                                .withConsentId("AT999999201912171011121230023456789")
                                .withMeteringPointId("AT9999990699900000000000206868100")
                                .withResponseCode(List.of(99))
                                .build()
                )).build();
    }

    @Test
    public void testEmptyString() {
        // Assign empty string to required attributes
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMessageId(""));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConversationId(""));
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        // 35 Characters
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMessageId("thisIsARandomStringWithOver35Characters"));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConversationId("thisIsARandomStringWithOver35Characters"));
    }

    @Test
    public void testEmptyList() {
        // Assign empty list to required attributes
        List<ResponseDataType> responseData = new ArrayList<>();
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withResponseData(responseData));
    }

    @Test
    public void testIllegalStateException() {
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        // Assign no attributes
        assertThrows(IllegalStateException.class, processDirBuilder::build);

        // Assign only one required attribute
        assertThrows(IllegalStateException.class, () -> processDirBuilder
                .withMessageId("Test")
                .build());

        // Assign only two required attribute
        assertThrows(IllegalStateException.class, () -> processDirBuilder
                .withConversationId("Test")
                .build());

        // Assign only three required attribute
        assertThrows(IllegalStateException.class, () -> processDirBuilder
                .withCMRequestId("Test")
                .build());
    }
}
