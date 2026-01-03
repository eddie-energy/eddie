package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;


class DatadisAccountingPointEnvelopeProviderTest {
    @Test
    void testGetAccountingPointEnvelopeFlux_publishesDocuments() throws Exception {
        // Given
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
        IntermediateAPMDFactory factory = new IntermediateAPMDFactory(
                datadisConfig,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        IdentifiableAccountingPointData identifiableAccountingPointData = IntermediateAccountingPointMarketDocumentTest.identifiableAccountingPointData();
        EnergyDataStreams streams = new EnergyDataStreams();
        var provider = new DatadisAccountingPointEnvelopeProvider(streams, factory);

        // When
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(() -> {
                        streams.publish(identifiableAccountingPointData);
                        streams.close();
                    })
                    .expectNextCount(1)
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }
}
