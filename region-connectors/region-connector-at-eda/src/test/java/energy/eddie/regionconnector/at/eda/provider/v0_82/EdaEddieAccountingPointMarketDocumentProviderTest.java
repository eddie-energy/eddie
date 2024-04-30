package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.EdaResourceLoader;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EdaEddieAccountingPointMarketDocumentProviderTest {

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
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME
                )
        );

        Sinks.Many<IdentifiableMasterData> identifiableMasterDataSink = Sinks.many().unicast().onBackpressureBuffer();
        var edaEddieAccountingPointMarketDocumentProvider = new EdaEddieAccountingPointMarketDocumentProvider(
                identifiableMasterDataSink.asFlux(),
                factory
        );

        StepVerifier.create(edaEddieAccountingPointMarketDocumentProvider.getEddieAccountingPointMarketDocumentStream())
                    .then(() -> identifiableMasterDataSink.tryEmitNext(identifiableMasterData))
                    .expectNextCount(1)
                    .then(identifiableMasterDataSink::tryEmitComplete)
                    .verifyComplete();


        edaEddieAccountingPointMarketDocumentProvider.close();
    }
}
