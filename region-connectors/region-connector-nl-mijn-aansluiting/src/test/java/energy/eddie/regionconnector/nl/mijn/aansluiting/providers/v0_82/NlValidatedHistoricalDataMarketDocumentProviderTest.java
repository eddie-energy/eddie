package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlValidatedHistoricalDataMarketDocumentProviderTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "fallback"
    );
    private final JsonResourceObjectMapper<List<MijnAansluitingResponse>> mapper = new JsonResourceObjectMapper<>(new TypeReference<>() {});
    private final MijnAansluitingConfiguration config = new MijnAansluitingConfiguration(
            "",
            "",
            new ClientID("client-id"),
            new Scope(),
            URI.create("http://localhost"), "jwt", null
    );
    private final MijnAansluitingPermissionRequest pr = new MijnAansluitingPermissionRequest("pid",
                                                                                             "cid",
                                                                                             "dnid",
                                                                                             PermissionProcessStatus.ACCEPTED,
                                                                                             "",
                                                                                             "",
                                                                                             null,
                                                                                             null,
                                                                                             null,
                                                                                             null, "11", "999AB");
    @Mock
    private PollingService pollingService;

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_emits() throws IOException {
        // Given
        var json = mapper.loadTestJson("single_consumption_data.json");
        when(pollingService.identifiableMeteredDataFlux())
                .thenReturn(Flux.just(new IdentifiableMeteredData(pr, json)));
        var vhdProvider = new NlValidatedHistoricalDataEnvelopeProvider(pollingService, cimConfig, config);

        // When
        var res = vhdProvider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();

        // Clean-Up
        vhdProvider.close();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_emitsNothing_onInvalidDocuments() throws IOException {
        // Given
        var json = mapper.loadTestJson("invalid_obis_code_consumption_data.json");
        when(pollingService.identifiableMeteredDataFlux())
                .thenReturn(Flux.just(new IdentifiableMeteredData(pr, json)));
        var vhdProvider = new NlValidatedHistoricalDataEnvelopeProvider(pollingService, cimConfig, config);

        // When
        var res = vhdProvider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .verifyComplete();

        // Clean-Up
        vhdProvider.close();
    }
}