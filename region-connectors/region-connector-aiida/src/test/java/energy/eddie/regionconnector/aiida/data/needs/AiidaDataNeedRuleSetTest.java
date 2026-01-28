package energy.eddie.regionconnector.aiida.data.needs;

import org.junit.jupiter.api.Test;

import static energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule;
import static energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule.AiidaDataNeedTypes.INBOUND;
import static energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule.AiidaDataNeedTypes.OUTBOUND;
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
                        new AiidaDataNeedRule(INBOUND),
                        new AiidaDataNeedRule(OUTBOUND)
                );
    }
}