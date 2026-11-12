// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v1_12;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82.NlValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.core.type.TypeReference;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlValidatedHistoricalDataMarketDocumentProviderV112Test {
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
    void testGetValidatedHistoricalDataMarketDocumentsStream_emits() {
        // Given
        var json = mapper.loadTestJson("single_consumption_data.json");
        when(pollingService.identifiableMeteredDataFlux())
                .thenReturn(Flux.just(new IdentifiableMeteredData(pr, json)));
        var provider = new NlValidatedHistoricalDataMarketDocumentProviderV112(pollingService, cimConfig, config);

        // When
        var res = provider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_emitsNothing_onInvalidDocuments() {
        // Given
        var json = mapper.loadTestJson("invalid_obis_code_consumption_data.json");
        when(pollingService.identifiableMeteredDataFlux())
                .thenReturn(Flux.just(new IdentifiableMeteredData(pr, json)));
        var provider = new NlValidatedHistoricalDataMarketDocumentProviderV112(pollingService, cimConfig, config);

        // When
        var res = provider.getValidatedHistoricalDataMarketDocumentsStream();

        // Then
        StepVerifier.create(res)
                    .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "single_consumption_data.json",
            "single_consumption_data_multiple_meters.json",
            "consumption_data.json",
            "gas_consumption_data.json"
    })
    void testGetValidatedHistoricalDataMarketDocumentsStream_isEquivalentToV082AndValid(String file) throws Exception {
        // Given
        var json = mapper.loadTestJson(file);
        when(pollingService.identifiableMeteredDataFlux())
                .thenReturn(Flux.just(new IdentifiableMeteredData(pr, json)));
        var v082Provider = new NlValidatedHistoricalDataEnvelopeProvider(pollingService, cimConfig, config);
        var v112Provider = new NlValidatedHistoricalDataMarketDocumentProviderV112(pollingService, cimConfig, config);

        // When
        var v082 = v082Provider.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();
        var v112 = v112Provider.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();

        // Then
        assertNotNull(v082);
        assertNotNull(v112);
        assertEquals(v082.size(), v112.size());
        var serde = new XmlMessageSerde();
        for (int i = 0; i < v082.size(); i++) {
            assertEquivalent(v082.get(i), v112.get(i));
            var xml = serde.serialize(v112.get(i));
            assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                       "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
        }
    }

    private static void assertEquivalent(ValidatedHistoricalDataEnvelope v082, Envelope v112) {
        var doc082 = v082.getValidatedHistoricalDataMarketDocument();
        var doc112 = v112.getVHDMarketDocument();
        assertEquals("NL", v112.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        assertEquals(doc082.getSenderMarketParticipantMRID().getValue(), doc112.getSenderMarketParticipantMRID().getValue());
        assertEquals(doc082.getReceiverMarketParticipantMRID().getValue(),
                     doc112.getReceiverMarketParticipantMRID().getValue());
        var series082 = doc082.getTimeSeriesList().getTimeSeries();
        var series112 = doc112.getTimeSeries();
        assertEquals(series082.size(), series112.size());
        for (int i = 0; i < series082.size(); i++) {
            var ts082 = series082.get(i);
            var ts112 = series112.get(i);
            assertEquals(ts082.getRegisteredResource().getMRID(), ts112.getRegisteredResourceMRID().getValue());
            assertEquals(ts082.getFlowDirectionDirection().value(), ts112.getFlowDirectionDirection());
            assertEquals(ts082.getBusinessType().value(), ts112.getBusinessType());
            assertEquals(ts082.getEnergyMeasurementUnitName().value(), ts112.getEnergyMeasurementUnitName());
            assertEquals(ts082.getMarketEvaluationPointMRID().getValue(), ts112.getAccountingPointMRID().getValue());
            var period082 = ts082.getSeriesPeriodList().getSeriesPeriods().getFirst();
            var period112 = ts112.getSeriesPeriods().getFirst();
            assertEquals(period082.getTimeInterval().getStart(), period112.getTimeInterval().getStart());
            assertEquals(period082.getTimeInterval().getEnd(), period112.getTimeInterval().getEnd());
            var points082 = period082.getPointList().getPoints();
            var points112 = period112.getPoints();
            assertEquals(points082.size(), points112.size());
            for (int j = 0; j < points082.size(); j++) {
                assertEquals(0, points082.get(j).getEnergyQuantityQuantity()
                                             .compareTo(points112.get(j).getEnergyQuantityQuantity()));
                assertEquals(j + 1, points112.get(j).getPosition());
                assertEquals(points082.get(j).getEnergyQuantityQuality().value(),
                             points112.get(j).getEnergyQuantityQuality());
            }
        }
    }
}
