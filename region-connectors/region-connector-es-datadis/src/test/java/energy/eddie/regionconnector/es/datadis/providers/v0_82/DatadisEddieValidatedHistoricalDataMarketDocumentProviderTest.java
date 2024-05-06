package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

class DatadisEddieValidatedHistoricalDataMarketDocumentProviderTest {

    @Test
    void testGetEddieValidatedHistoricalDataMarketDocumentStream_publishesDocuments() throws Exception {
        // Given
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId", "clientSecret", "basepath");
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                datadisConfig,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        IdentifiableMeteringData identifiableMeteringData = IntermediateValidatedHistoricalDocumentTest.identifiableMeterReading(
                false);
        var provider = new DatadisEddieValidatedHistoricalDataMarketDocumentProvider(testPublisher.flux(), factory);

        // When
        StepVerifier.create(provider.getEddieValidatedHistoricalDataMarketDocumentStream())
                    .then(() -> {
                        testPublisher.emit(identifiableMeteringData);
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }
}
