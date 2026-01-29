// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoints;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.core.type.TypeReference;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class NlAccountingPointDataEnvelopeProviderTest {
    private final JsonResourceObjectMapper<MeteringPoints> apMapper = new JsonResourceObjectMapper<>(new TypeReference<>() {});
    @Spy
    private final MijnAansluitingConfiguration config = new MijnAansluitingConfiguration(
            "key-id",
            "https://localhost",
            new ClientID("client-id"),
            new Scope(),
            URI.create("http://localhost"),
            "api-token",
            URI.create("http://localhost")
    );
    @Mock
    private PollingService pollingService;

    @Test
    void toAp_returnsAccountingPoints() {
        // Given
        var meteringPoints = apMapper.loadTestJson("codeboek_response.json");
        var pr = createPermissionRequest();
        var identifiableAccountingPointData = new IdentifiableAccountingPointData(pr,
                                                                                  meteringPoints.getMeteringPoints());
        when(pollingService.identifiableAccountingPointDataFlux())
                .thenReturn(Flux.just(identifiableAccountingPointData));
        var provider = new NlAccountingPointDataEnvelopeProvider(pollingService, config);

        // When
        var res = provider.getAccountingPointEnvelopeFlux();

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();

        // Clean Up
        provider.close();
    }

    private static MijnAansluitingPermissionRequest createPermissionRequest() {
        var today = LocalDate.now(ZoneOffset.UTC);
        return new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "state",
                "codeVerifier",
                ZonedDateTime.now(ZoneOffset.UTC),
                today,
                today,
                Granularity.P1D,
                "11",
                "9999AB"
        );
    }
}