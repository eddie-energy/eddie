package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p11;

import energy.eddie.regionconnector.at.eda.dto.ResponseData;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarshallerConfig.class)
class EdaCMNotification01p11InboundMessageFactoryTest {

    @Autowired
    private Jaxb2Marshaller marshaller;

    private static Stream<Arguments> inputStreams() {
        ClassLoader classLoader = EdaCMNotification01p11InboundMessageFactoryTest.class.getClassLoader();
        var answer = classLoader.getResourceAsStream("xsd/cmnotification/_01p11/answer_ccmo.xml");
        var rejected = classLoader.getResourceAsStream("xsd/cmnotification/_01p11/rejected_ccmo.xml");
        var accepted = classLoader.getResourceAsStream("xsd/cmnotification/_01p11/accepted_ccmo.xml");

        return Stream.of(
                Arguments.of(answer, "BI2AWUO2", "EPXXXXXXT1712068829927", null, 99),
                Arguments.of(rejected, "B2QG42ZU", "EPXXXXXXT1712065219565", null, 178),
                Arguments.of(accepted, "BI2AWUO2", "EPXXXXXXT1712068829927", "ATXXXXXX20240402164046426BI2AWUO2", 175)
        );
    }

    public static Stream<Arguments> activeDates() {
        return Stream.of(
                Arguments.of(LocalDate.of(2023, 1, 7)),
                Arguments.of(LocalDate.of(2024, 4, 7)),
                Arguments.of(LocalDate.of(2025, 6, 8)),
                Arguments.of(LocalDate.of(2028, 7, 9))
        );
    }

    @ParameterizedTest
    @MethodSource("inputStreams")
    void parseInputStream_parsesMessagesAsExpected(
            InputStream inputStream,
            String cmRequestId,
            String conversationId,
            @Nullable String consentId,
            int responseCode
    ) throws IOException {
        // Given
        var factory = new EdaCMNotification01p11InboundMessageFactory(marshaller);

        // When
        var notification = factory.parseInputStream(inputStream);
        inputStream.close();

        // Then
        ResponseData responseData = notification.responseData().getFirst();
        assertAll(
                () -> assertEquals(cmRequestId, notification.cmRequestId()),
                () -> assertEquals(conversationId, notification.conversationId()),
                () -> assertEquals(1, notification.responseData().size()),
                () -> assertEquals("ATXXXXXX00000000000000000XXXXXXXX", responseData.meteringPoint()),
                () -> assertEquals(consentId, responseData.consentId()),
                () -> assertEquals(1, responseData.responseCodes().size()),
                () -> assertEquals(responseCode, responseData.responseCodes().getFirst())
        );
    }

    @ParameterizedTest
    @MethodSource("activeDates")
    void isActive_returnsTrue(LocalDate date) {
        // Given
        var factory = new EdaCMNotification01p11InboundMessageFactory(marshaller);

        // When
        var active = factory.isActive(date);

        // Then
        assertTrue(active);
    }
}
