// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.SeriesPeriodBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.TimeSeriesBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnerginetValidatedHistoricalDataEnvelopeProviderTest {
    private static MyEnergyDataMarketDocument myEnergyDataMarketDocument;
    private static ValidatedHistoricalDataMarketDocumentBuilderFactory factory;
    private static IdentifiableApiResponse apiResponse;
    @Mock
    private EnergyDataStreams streams;
    @Mock
    private ValidatedHistoricalDataMarketDocumentBuilderFactory mockFactory;

    @SuppressWarnings("DataFlowIssue")
    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        var classLoader = EnerginetValidatedHistoricalDataEnvelopeProviderTest.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            var response = objectMapper.readValue(is, MyEnergyDataMarketDocumentResponseListApiResponse.class);
            myEnergyDataMarketDocument = response.getResult().getFirst().getMyEnergyDataMarketDocument();
        }

        factory = new ValidatedHistoricalDataMarketDocumentBuilderFactory(
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId"
                ),
                new TimeSeriesBuilderFactory(new SeriesPeriodBuilderFactory())
        );

        var myEnergyDataMarketDocumentResponse = new MyEnergyDataMarketDocumentResponse()
                .myEnergyDataMarketDocument(myEnergyDataMarketDocument);
        var permissionRequest = new EnerginetPermissionRequestBuilder()
                .setPermissionId("permissionId")
                .setConnectionId("connectionId")
                .setDataNeedId("dataNeedId")
                .setMeteringPoint("meteringPointId")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                .build();
        apiResponse = new IdentifiableApiResponse(permissionRequest, myEnergyDataMarketDocumentResponse);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void getValidatedHistoricalDataMarketDocumentStream_producesDocumentsWhenTestJsonIsUsed() {
        // Given
        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();
        when(streams.getValidatedHistoricalDataStream()).thenReturn(testPublisher.flux());

        var provider = new EnerginetValidatedHistoricalDataEnvelopeProvider(streams, factory);

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
        when(mockFactory.create()).thenReturn(builder);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();
        when(streams.getValidatedHistoricalDataStream()).thenReturn(testPublisher.flux());

        try (var provider = new EnerginetValidatedHistoricalDataEnvelopeProvider(streams, mockFactory)) {

            // When & Then
            StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                        .then(() -> {
                            testPublisher.emit(apiResponse);
                            testPublisher.complete();
                        })
                        .verifyComplete();

            verify(mockFactory).create();
            verify(builder).withMyEnergyDataMarketDocument(any());
            verifyNoMoreInteractions(builder, mockFactory);
        }
    }
}
