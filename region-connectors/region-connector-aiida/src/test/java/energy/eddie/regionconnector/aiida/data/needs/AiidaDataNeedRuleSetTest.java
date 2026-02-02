package energy.eddie.regionconnector.aiida.data.needs;

import energy.eddie.dataneeds.rules.DataNeedRule.OutboundAiidaDataNeedRule;
import org.junit.jupiter.api.Test;

import static energy.eddie.dataneeds.rules.DataNeedRule.InboundAiidaDataNeedRule;
import static org.assertj.core.api.Assertions.assertThat;

class AiidaDataNeedRuleSetTest {

    @Test
    void givenAiidaDataNeedRuleSet_whenGettingRules_thenReturnsOnlyAiidaDataNeedRules() {
        // Given
        var ruleSet = new AiidaDataNeedRuleSet();

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new InboundAiidaDataNeedRule(),
                        new OutboundAiidaDataNeedRule()
                );
    }
}