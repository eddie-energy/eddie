package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;


class DatadisEddieAccountingPointMarketDocumentProviderTest {


    @Test
    void testGetEddieAccountingPointMarketDocumentStream_publishesDocuments() throws Exception {
        // Given
        TestPublisher<IdentifiableAccountingPointData> testPublisher = TestPublisher.create();
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId", "clientSecret", "basepath");
        IntermediateAPMDFactory factory = new IntermediateAPMDFactory(
                datadisConfig,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        IdentifiableAccountingPointData identifiableAccountingPointData = IntermediateAccountingPointMarketDocumentTest.identifiableAccountingPointData();
        var provider = new DatadisEddieAccountingPointMarketDocumentProvider(testPublisher.flux(), factory);

        // When
        StepVerifier.create(provider.getEddieAccountingPointMarketDocumentStream())
                    .then(() -> {
                        testPublisher.emit(identifiableAccountingPointData);
                        testPublisher.complete();
                    })
                    .expectNextCount(1)
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }
}
