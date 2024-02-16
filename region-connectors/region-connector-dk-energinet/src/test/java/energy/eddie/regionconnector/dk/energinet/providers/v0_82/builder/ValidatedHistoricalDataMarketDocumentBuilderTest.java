package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.MessageTypeList;
import energy.eddie.cim.v0_82.vhd.ProcessTypeList;
import energy.eddie.cim.v0_82.vhd.RoleTypeList;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("DataFlowIssue")
class ValidatedHistoricalDataMarketDocumentBuilderTest {

    static MyEnergyDataMarketDocument myEnergyDataMarketDocument;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        try (InputStream is = ValidatedHistoricalDataMarketDocumentBuilderTest.class.getClassLoader().getResourceAsStream("MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            MyEnergyDataMarketDocumentResponseListApiResponse response = objectMapper.readValue(is, MyEnergyDataMarketDocumentResponseListApiResponse.class);
            myEnergyDataMarketDocument = response.getResult().getFirst().getMyEnergyDataMarketDocument();
        }
    }

    @Test
    void withMyEnergyDataMarketDocument() {
        // Arrange
        String customerId = "customerId";
        CodingSchemeTypeList codingScheme = CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME;

        TimeSeriesBuilder timeSeriesBuilder = mock(TimeSeriesBuilder.class);
        when(timeSeriesBuilder.withTimeSeriesList(any())).thenReturn(timeSeriesBuilder);
        TimeSeriesBuilderFactory timeSeriesBuilderFactory = mock(TimeSeriesBuilderFactory.class);
        when(timeSeriesBuilderFactory.create()).thenReturn(timeSeriesBuilder);

        ValidatedHistoricalDataMarketDocumentBuilder builder = new ValidatedHistoricalDataMarketDocumentBuilder(
                customerId,
                codingScheme,
                timeSeriesBuilderFactory
        );

        // Act
        builder.withMyEnergyDataMarketDocument(myEnergyDataMarketDocument);
        var result = builder.build();

        // Assert
        assertEquals("80024a18-0001-ef00-b63f-84710c7967XX", result.getMRID());
        assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, result.getType());
        assertEquals(CommonInformationModelVersions.V0_82.version(), result.getRevisionNumber());
        assertEquals("2024-02-15T10:10:07Z", result.getCreatedDateTime());
        assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR, result.getSenderMarketParticipantMarketRoleType());
        assertEquals(RoleTypeList.CONSUMER, result.getReceiverMarketParticipantMarketRoleType());
        assertEquals(ProcessTypeList.REALISED, result.getProcessProcessType());
        assertEquals(CodingSchemeTypeList.GS1, result.getSenderMarketParticipantMRID().getCodingScheme());
        assertEquals("5790001330583", result.getSenderMarketParticipantMRID().getValue());
        assertEquals(codingScheme, result.getReceiverMarketParticipantMRID().getCodingScheme());
        assertEquals(customerId, result.getReceiverMarketParticipantMRID().getValue());
        assertEquals("2024-02-11T23:00:00Z", result.getPeriodTimeInterval().getStart());
        assertEquals("2024-02-13T23:00:00Z", result.getPeriodTimeInterval().getEnd());

        verify(timeSeriesBuilder).withTimeSeriesList(myEnergyDataMarketDocument.getTimeSeries());
        verify(timeSeriesBuilder).build();
        verify(timeSeriesBuilderFactory).create();
        verifyNoMoreInteractions(timeSeriesBuilder, timeSeriesBuilderFactory);
    }
}