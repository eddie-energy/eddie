package energy.eddie.regionconnector.cds.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.supported.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsDataNeedRuleSetTest {
    @Mock
    private CdsServerClientFactory factory;
    @Mock
    private CdsServerClient client;

    @Test
    void testSupportedEnergyTypes_returnsEnergyTypes() {
        // Given
        var ruleSet = new CdsDataNeedRuleSet(factory);
        when(factory.getAll()).thenReturn(Flux.just(client));
        when(client.masterData())
                .thenReturn(Mono.just(
                        new CdsServerMasterData(
                                "CDS Server",
                                "1",
                                URI.create("http://localhost"),
                                Set.of(
                                        new Coverage(EnergyType.ELECTRICITY, "us"),
                                        new Coverage(EnergyType.HYDROGEN, "us")
                                )
                        )
                ));
        var granularities = Arrays.asList(Granularity.values());

        // When
        var res = ruleSet.dataNeedRules();
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new AccountingPointDataNeedRule(),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, granularities),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.HYDROGEN, granularities)
                );
    }
}