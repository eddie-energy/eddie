package energy.eddie.dataneeds.rules;

import energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule.AiidaDataNeedTypes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DataNeedRuleTest {

    @ParameterizedTest
    @EnumSource(AiidaDataNeedTypes.class)
    void givenAiidaDataNeedRule_whenClass_thenReturnsCorrect(AiidaDataNeedTypes type) {
        // Given
        var rule = new DataNeedRule.AiidaDataNeedRule(type);

        // When & Then
        assertThat(rule.getDataNeedClass()).isEqualTo(type.clazz());
        assertThat(rule.getType()).isEqualTo(type.value());
    }
}