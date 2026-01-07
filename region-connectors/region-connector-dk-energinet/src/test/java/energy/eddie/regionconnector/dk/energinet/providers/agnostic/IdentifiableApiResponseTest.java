package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("DataFlowIssue")
class IdentifiableApiResponseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static MyEnergyDataMarketDocumentResponse response;

    @BeforeAll
    static void setUp() throws IOException {
        try (InputStream is = IdentifiableApiResponseTest.class.getClassLoader()
                                                               .getResourceAsStream(
                                                                       "MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            MyEnergyDataMarketDocumentResponseListApiResponse response = MAPPER.readValue(is,
                                                                                          MyEnergyDataMarketDocumentResponseListApiResponse.class);
            IdentifiableApiResponseTest.response = response.getResult().getFirst();
        }
    }

    @Test
    void extractMeterReadingEndDate_returnsExpected() {
        // Arrange
        IdentifiableApiResponse identifiableApiResponse = new IdentifiableApiResponse(null, response);

        var expected = ZonedDateTime.parse(
                                            response.getMyEnergyDataMarketDocument().getPeriodTimeInterval().getEnd(),
                                            DateTimeFormatter.ISO_DATE_TIME
                                    )
                                    .withZoneSameInstant(DK_ZONE_ID)
                                    .toLocalDate();

        // Act
        var result = identifiableApiResponse.meterReadingEndDate();

        // Assert
        assertEquals(expected, result);
    }
}
