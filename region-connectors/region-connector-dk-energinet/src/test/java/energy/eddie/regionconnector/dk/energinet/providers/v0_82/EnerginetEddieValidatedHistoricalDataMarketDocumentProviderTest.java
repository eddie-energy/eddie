package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.SeriesPeriodBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.TimeSeriesBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings({"resource", "OptionalGetWithoutIsPresent"})
class EnerginetEddieValidatedHistoricalDataMarketDocumentProviderTest {

    static MyEnergyDataMarketDocument myEnergyDataMarketDocument;

    static ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory;
    static IdentifiableApiResponse apiResponse;

    @SuppressWarnings("DataFlowIssue")
    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        try (InputStream is = EnerginetEddieValidatedHistoricalDataMarketDocumentProviderTest.class.getClassLoader().getResourceAsStream("MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            MyEnergyDataMarketDocumentResponseListApiResponse response = objectMapper.readValue(is, MyEnergyDataMarketDocumentResponseListApiResponse.class);
            myEnergyDataMarketDocument = response.getResult().getFirst().getMyEnergyDataMarketDocument();
        }

        validatedHistoricalDataMarketDocumentBuilderFactory = new ValidatedHistoricalDataMarketDocumentBuilderFactory(
                new MyEnerginetConfiguration(),
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                new TimeSeriesBuilderFactory(new SeriesPeriodBuilderFactory())
        );

        MyEnergyDataMarketDocumentResponse myEnergyDataMarketDocumentResponse = new MyEnergyDataMarketDocumentResponse();
        myEnergyDataMarketDocumentResponse.setMyEnergyDataMarketDocument(myEnergyDataMarketDocument);
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", "dataNeedId");
        apiResponse = new IdentifiableApiResponse(
                permissionRequest,
                myEnergyDataMarketDocumentResponse
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_producesDocumentWhenTestJsonIsUsed() {
        // Given

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        var provider = new EnerginetEddieValidatedHistoricalDataMarketDocumentProvider(testPublisher.flux(), validatedHistoricalDataMarketDocumentBuilderFactory);

        // When & Then
        StepVerifier.create(provider.getEddieValidatedHistoricalDataMarketDocumentStream())
                .then(() -> {
                    testPublisher.emit(apiResponse);
                    testPublisher.complete();
                })
                .assertNext(document -> {
                    assertEquals(apiResponse.permissionRequest().permissionId(), document.permissionId().get());
                    assertEquals(apiResponse.permissionRequest().connectionId(), document.connectionId().get());
                    assertEquals(apiResponse.permissionRequest().dataNeedId(), document.dataNeedId().get());

                    assertEquals(myEnergyDataMarketDocument.getPeriodTimeInterval().getStart(), document.marketDocument().getPeriodTimeInterval().getStart());
                    assertEquals(myEnergyDataMarketDocument.getPeriodTimeInterval().getEnd(), document.marketDocument().getPeriodTimeInterval().getEnd());
                    assertEquals(myEnergyDataMarketDocument.getTimeSeries().size(), document.marketDocument().getTimeSeriesList().getTimeSeries().size());
                    assertEquals(myEnergyDataMarketDocument.getTimeSeries().getFirst().getPeriod().size(), document.marketDocument().getTimeSeriesList().getTimeSeries().getFirst().getSeriesPeriodList().getSeriesPeriods().size());
                    assertEquals(myEnergyDataMarketDocument.getTimeSeries().getLast().getPeriod().size(), document.marketDocument().getTimeSeriesList().getTimeSeries().getLast().getSeriesPeriodList().getSeriesPeriods().size());
                })
                .verifyComplete();
    }

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_producesNothingIfMappingFails() throws Exception {
        // Given
        ValidatedHistoricalDataMarketDocumentBuilder builder = mock(ValidatedHistoricalDataMarketDocumentBuilder.class);
        doThrow(new RuntimeException("Test exception")).when(builder).withMyEnergyDataMarketDocument(any());
        ValidatedHistoricalDataMarketDocumentBuilderFactory factory = mock(ValidatedHistoricalDataMarketDocumentBuilderFactory.class);
        when(factory.create()).thenReturn(builder);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        try (var provider = new EnerginetEddieValidatedHistoricalDataMarketDocumentProvider(testPublisher.flux(), factory)) {

            // When & Then
            StepVerifier.create(provider.getEddieValidatedHistoricalDataMarketDocumentStream())
                    .then(() -> {
                        testPublisher.emit(apiResponse);
                        testPublisher.complete();
                    })
                    .verifyComplete();

            verify(factory).create();
            verify(builder).withMyEnergyDataMarketDocument(any());
            verifyNoMoreInteractions(builder, factory);
        }
    }

    private static class MyEnerginetConfiguration implements EnerginetConfiguration {
        @Override
        public String customerBasePath() {
            return "customerBasePath";
        }

        @Override
        public String customerId() {
            return "customerId";
        }
    }
}
