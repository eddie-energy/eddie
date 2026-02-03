package energy.eddie.dataneeds.rules;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AllowMultipleDataNeedsRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataNeedRuleSetTest {

    @Test
    void givenAccountingPointDataNeedRule_whenHasRuleAccountingPointDataNeedRule_thenReturnTrue() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new AccountingPointDataNeedRule());

        // When
        var res = ruleSet.hasRule(new AccountingPointDataNeedRule());

        // Then
        assertTrue(res);
    }

    @Test
    void givenAccountingPointDataNeedRule_whenHasRuleForAccountingPoint_thenReturnTrue() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new AccountingPointDataNeedRule());

        // When
        var res = ruleSet.hasRuleFor(new AccountingPointDataNeed());

        // Then
        assertTrue(res);
    }


    @Test
    void givenAccountingPointDataNeedRule_whenHasRuleForAiidaDataNeed_thenReturnFalse() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new AccountingPointDataNeedRule());

        // When
        var res = ruleSet.hasRuleFor(new OutboundAiidaDataNeed());

        // Then
        assertFalse(res);
    }

    @Test
    void givenAllowMultipleDataNeedsRule_whenHasRuleForAiidaDataNeed_thenReturnFalse() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new AllowMultipleDataNeedsRule());

        // When
        var res = ruleSet.hasRuleFor(new OutboundAiidaDataNeed());

        // Then
        assertFalse(res);
    }


    @Test
    void givenAllowMultipleDataNeedsAndAccountingPointDataNeedRule_whenDataNeedRulesWithSpecificDataNeedRuleClass_thenAccountingPointDataNeedRules() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new AllowMultipleDataNeedsRule(), new AccountingPointDataNeedRule());

        // When
        var res = ruleSet.dataNeedRules(AccountingPointDataNeedRule.class);

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(new AccountingPointDataNeedRule());
    }


    @Test
    void givenAllowMultipleDataNeedsAndAccountingPointDataNeedRule_whenSupportedDataNeeds_thenReturnCorrectTypes() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new AllowMultipleDataNeedsRule(), new AccountingPointDataNeedRule());

        // When
        var res = ruleSet.supportedDataNeeds();

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(AccountingPointDataNeed.DISCRIMINATOR_VALUE);
    }
}