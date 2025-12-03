package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

class EnerginetValidatedHistoricalDataMarketDocumentProviderTest {

    @Test
    void givenValidatedHistoricalData_whenReceived_thenPublishValidatedHistoricalDataMarketDocument() throws IOException {
        // Given
        var streams = new EnergyDataStreams();
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var provider = new EnerginetValidatedHistoricalDataMarketDocumentProvider(streams, cimConfig);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "token",
                                                now.toLocalDate(),
                                                now.toLocalDate(),
                                                Granularity.P1D,
                                                null,
                                                PermissionProcessStatus.ACCEPTED,
                                                now,
                                                null);
        var response = DtoLoader.loadValidatedHistoricalData().getResult();

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(new IdentifiableApiResponse(pr, Objects.requireNonNull(response).getFirst()));
                        streams.close();
                    })
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }
}