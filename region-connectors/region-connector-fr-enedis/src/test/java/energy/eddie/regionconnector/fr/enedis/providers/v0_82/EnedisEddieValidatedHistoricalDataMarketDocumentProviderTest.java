package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisEddieValidatedHistoricalDataMarketDocumentProviderTest {
    @Test
    void testGetEddieValidatedHistoricalDataMarketDocumentStream_publishesDocuments() throws Exception {
        // Given
        var meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK);
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.granularity()).thenReturn(Granularity.PT30M);

        var identifiableMeterReading = new IdentifiableMeterReading(permissionRequest, meterReading);
        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();
        PlainEnedisConfiguration enedisConfiguration = new PlainEnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path",
                24);
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                enedisConfiguration,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        var provider = new EnedisEddieValidatedHistoricalDataMarketDocumentProvider(testPublisher.flux(), factory);

        // When
        StepVerifier.create(provider.getEddieValidatedHistoricalDataMarketDocumentStream())
                    .then(() -> {
                        testPublisher.emit(identifiableMeterReading);
                        testPublisher.complete();
                    })
                    .assertNext(vhd -> assertEquals("pid", vhd.permissionId()))
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }
}
