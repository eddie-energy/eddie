package energy.eddie.regionconnector.es.datadis.data.needs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatadisDataNeedRuleSetTest {
    @Test
    void testRuleSet() {
        // Given
        var ruleSet = new DatadisDataNeedRuleSet();

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res).hasSize(2);
    }
}