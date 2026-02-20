// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.rules;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule.*;
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
    void givenMultipleDataNeedRules_whenDataNeedRulesWithAiidaDataNeedRuleClass_thenDataNeedRules() {
        // Given
        DataNeedRuleSet ruleSet = () -> List.of(new InboundAiidaDataNeedRule(), new OutboundAiidaDataNeedRule(), new AllowMultipleDataNeedsRule());

        // When
        var res = ruleSet.dataNeedRules(SpecificDataNeedRule.class);

        // Then
        assertThat(res).containsAll(List.of(new InboundAiidaDataNeedRule(), new OutboundAiidaDataNeedRule()));
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