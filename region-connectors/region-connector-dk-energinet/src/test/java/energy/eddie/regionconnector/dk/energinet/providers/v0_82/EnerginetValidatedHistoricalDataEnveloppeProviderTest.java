package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings({"resource"})
class EnerginetValidatedHistoricalDataEnveloppeProviderTest {

    static MyEnergyDataMarketDocument myEnergyDataMarketDocument;

    static ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory;
    static IdentifiableApiResponse apiResponse;

    @SuppressWarnings("DataFlowIssue")
    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        try (InputStream is = EnerginetValidatedHistoricalDataEnveloppeProviderTest.class.getClassLoader()
                                                                                         .getResourceAsStream(
                                                                                                           "MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            MyEnergyDataMarketDocumentResponseListApiResponse response = objectMapper.readValue(is,
                                                                                                MyEnergyDataMarketDocumentResponseListApiResponse.class);
            myEnergyDataMarketDocument = response.getResult().getFirst().getMyEnergyDataMarketDocument();
        }

        validatedHistoricalDataMarketDocumentBuilderFactory = new ValidatedHistoricalDataMarketDocumentBuilderFactory(
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId"),
                new TimeSeriesBuilderFactory(new SeriesPeriodBuilderFactory())
        );

        MyEnergyDataMarketDocumentResponse myEnergyDataMarketDocumentResponse = new MyEnergyDataMarketDocumentResponse();
        myEnergyDataMarketDocumentResponse.setMyEnergyDataMarketDocument(myEnergyDataMarketDocument);
        var permissionRequest = new EnerginetPermissionRequest(
                "permissionId",
                "connectionId",
                "dataNeedId",
                "meteringPointId",
                "refreshToken",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.PT1H,
                "accessToken",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        apiResponse = new IdentifiableApiResponse(
                permissionRequest,
                myEnergyDataMarketDocumentResponse
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void getValidatedHistoricalDataMarketDocumentStream_producesDocumentsWhenTestJsonIsUsed() {
        // Given
        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        var provider = new EnerginetValidatedHistoricalDataEnveloppeProvider(testPublisher.flux(),
                                                                             validatedHistoricalDataMarketDocumentBuilderFactory);

        // When & Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        testPublisher.emit(apiResponse);
                        testPublisher.complete();
                    })
                    .assertNext(document -> {
                        var header = document.getMessageDocumentHeader()
                                             .getMessageDocumentHeaderMetaInformation();
                        assertEquals(apiResponse.permissionRequest().permissionId(), header.getPermissionid());
                        assertEquals(apiResponse.permissionRequest().connectionId(), header.getConnectionid());
                        assertEquals(apiResponse.permissionRequest().dataNeedId(), header.getDataNeedid());

                        assertEquals(myEnergyDataMarketDocument.getPeriodTimeInterval().getStart(),
                                     document.getValidatedHistoricalDataMarketDocument()
                                             .getPeriodTimeInterval()
                                             .getStart());
                        assertEquals(myEnergyDataMarketDocument.getPeriodTimeInterval().getEnd(),
                                     document.getValidatedHistoricalDataMarketDocument()
                                             .getPeriodTimeInterval()
                                             .getEnd());
                        assertEquals(myEnergyDataMarketDocument.getTimeSeries().size(),
                                     document.getValidatedHistoricalDataMarketDocument()
                                             .getTimeSeriesList()
                                             .getTimeSeries()
                                             .size());
                        assertEquals(myEnergyDataMarketDocument.getTimeSeries().getFirst().getPeriod().size(),
                                     document.getValidatedHistoricalDataMarketDocument()
                                             .getTimeSeriesList()
                                             .getTimeSeries()
                                             .getFirst()
                                             .getSeriesPeriodList()
                                             .getSeriesPeriods()
                                             .size());
                        assertEquals(myEnergyDataMarketDocument.getTimeSeries().getLast().getPeriod().size(),
                                     document.getValidatedHistoricalDataMarketDocument()
                                             .getTimeSeriesList()
                                             .getTimeSeries()
                                             .getLast()
                                             .getSeriesPeriodList()
                                             .getSeriesPeriods()
                                             .size());
                    })
                    .verifyComplete();
    }

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_producesNothingIfMappingFails() throws Exception {
        // Given
        ValidatedHistoricalDataMarketDocumentBuilder builder = mock(ValidatedHistoricalDataMarketDocumentBuilder.class);
        doThrow(new RuntimeException("Test exception")).when(builder).withMyEnergyDataMarketDocument(any());
        ValidatedHistoricalDataMarketDocumentBuilderFactory factory = mock(
                ValidatedHistoricalDataMarketDocumentBuilderFactory.class);
        when(factory.create()).thenReturn(builder);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        try (var provider = new EnerginetValidatedHistoricalDataEnveloppeProvider(testPublisher.flux(),
                                                                                  factory)) {

            // When & Then
            StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
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
}
