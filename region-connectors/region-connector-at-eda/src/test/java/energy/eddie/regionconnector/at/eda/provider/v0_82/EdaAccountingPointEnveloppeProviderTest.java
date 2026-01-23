// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.EdaResourceLoader;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaAccountingPointEnvelopeProviderTest {
    @Mock
    private IdentifiableStreams streams;

    @Test
    void mapsIncomingIdentifiableMasterDataToAccountingPointMarketDocument() throws Exception {
        EdaMasterData edaMasterData = EdaResourceLoader.loadEdaMasterData();
        IdentifiableMasterData identifiableMasterData = new IdentifiableMasterData(
                edaMasterData,
                new SimplePermissionRequest(
                        "pid",
                        "cid",
                        "did"
                )
        );

        IntermediateAccountingPointMarketDocumentFactory factory = new IntermediateAccountingPointMarketDocumentFactory(
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId")
        );

        Sinks.Many<IdentifiableMasterData> identifiableMasterDataSink = Sinks.many().unicast().onBackpressureBuffer();
        when(streams.masterDataStream()).thenReturn(identifiableMasterDataSink.asFlux());
        var edaEddieAccountingPointMarketDocumentProvider = new EdaAccountingPointEnvelopeProvider(
                streams,
                factory
        );

        StepVerifier.create(edaEddieAccountingPointMarketDocumentProvider.getAccountingPointEnvelopeFlux())
                    .then(() -> identifiableMasterDataSink.tryEmitNext(identifiableMasterData))
                    .expectNextCount(1)
                    .then(identifiableMasterDataSink::tryEmitComplete)
                    .verifyComplete();


        edaEddieAccountingPointMarketDocumentProvider.close();
    }
}
