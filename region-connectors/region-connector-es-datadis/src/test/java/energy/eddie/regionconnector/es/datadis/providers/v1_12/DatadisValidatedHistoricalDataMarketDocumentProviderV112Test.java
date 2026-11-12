// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.es.datadis.providers.v1_04.DatadisValidatedHistoricalDataMarketDocumentProvider;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatadisValidatedHistoricalDataMarketDocumentProviderV112Test {
    private final DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
    private final PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
            "fallbackId"
    );

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_publishesDocuments() throws Exception {
        // Given
        var streams = new EnergyDataStreams();
        var provider = new DatadisValidatedHistoricalDataMarketDocumentProviderV112(streams, datadisConfig, cimConfig);
        var data = identifiableMeteringData();

        // When & Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(data);
                        streams.close();
                    })
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_isEquivalentToV104AndValid() throws Exception {
        // Given
        var streams = new EnergyDataStreams();
        var v104Future = new DatadisValidatedHistoricalDataMarketDocumentProvider(streams, datadisConfig, cimConfig)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();
        var v112Future = new DatadisValidatedHistoricalDataMarketDocumentProviderV112(streams, datadisConfig, cimConfig)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();

        // When
        streams.publish(identifiableMeteringData());
        streams.close();

        // Then
        var v104 = v104Future.get();
        var v112 = v112Future.get();
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        // The creation time is taken from the clock at mapping time, so it differs between versions
        assertEquals(List.of(),
                     ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104.getFirst(),
                                                                                  v112.getFirst(),
                                                                                  "createdDateTime"));
        var envelope = v112.getFirst();
        assertEquals("ES", envelope.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        var xml = new XmlMessageSerde().serialize(envelope);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    private static IdentifiableMeteringData identifiableMeteringData() throws Exception {
        var meteringData = MeteringDataProvider.loadMeteringDataShort();
        var pr = new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setMeteringPointId("mid")
                .setGranularity(Granularity.PT1H)
                .build();
        var intermediate = IntermediateMeteringData.fromMeteringData(meteringData).block();
        assertNotNull(intermediate);
        return new IdentifiableMeteringData(pr, intermediate);
    }
}
