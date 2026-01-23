// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class DatadisValidatedHistoricalDataEnvelopeProviderTest {

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_publishesDocuments() throws Exception {
        // Given
        EnergyDataStreams streams = new EnergyDataStreams();
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                datadisConfig,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        IdentifiableMeteringData identifiableMeteringData = IntermediateValidatedHistoricalDocumentTest.identifiableMeterReading(
                false);
        var provider = new DatadisValidatedHistoricalDataEnvelopeProvider(streams, factory);

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(identifiableMeteringData);
                        streams.close();
                    })
                    .expectNextCount(1)
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }
}
