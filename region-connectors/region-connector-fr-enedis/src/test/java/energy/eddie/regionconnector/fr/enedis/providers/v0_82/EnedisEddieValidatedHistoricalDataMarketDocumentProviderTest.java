package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                "/path"
        );
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                enedisConfiguration,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME
        );
        var provider = new EnedisEddieValidatedHistoricalDataMarketDocumentProvider(testPublisher.flux(), factory);

        // When
        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getEddieValidatedHistoricalDataMarketDocumentStream()))
                .then(() -> {
                    testPublisher.emit(identifiableMeterReading);
                    testPublisher.complete();
                })
                .assertNext(vhd -> {
                    assertTrue(vhd.permissionId().isPresent());
                    assertEquals("pid", vhd.permissionId().get());
                })
                .verifyComplete();

        // Clean-Up
        provider.close();
    }
}