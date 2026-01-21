package energy.eddie.core.services;

import energy.eddie.core.dtos.SupportedDataNeeds;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataNeedRuleSetRouterTest {
    @Test
    void givenDataNeedRuleSet_whenRouteToDataNeedRuleSet_thenReturnDataNeedRuleSets() {
        // Given
        var router = new DataNeedRuleSetRouter();
        DataNeedRuleSet dataNeedRuleSet = () -> List.of(new AccountingPointDataNeedRule());
        router.register("id", dataNeedRuleSet);

        // When
        var res = router.dataNeedRuleSets();

        // Then
        assertThat(res)
                .containsExactlyEntriesOf(
                        Map.of("id", dataNeedRuleSet)
                );
    }

    @Test
    void givenDataNeedRuleSetAndRegionConnectorId_whenRouteToDataNeedRuleSet_thenReturnDataNeedRuleSets() throws UnknownRegionConnectorException {
        // Given
        var router = new DataNeedRuleSetRouter();
        DataNeedRuleSet dataNeedRuleSet = () -> List.of(new AccountingPointDataNeedRule());
        router.register("id", dataNeedRuleSet);

        // When
        var res = router.dataNeedRuleSets("id");

        // Then
        assertThat(res).isEqualTo(dataNeedRuleSet);
    }

    @Test
    void givenUnknownRegionConnectorId_whenRouteToDataNeedRuleSets_thenThrowsUnknownRegionConnectorException() {
        // Given
        var router = new DataNeedRuleSetRouter();

        // When % Then
        assertThrows(UnknownRegionConnectorException.class, () -> router.dataNeedRuleSets("id"));
    }

    @Test
    void givenAccountingPointDataNeedRule_whenSupportedDataNeeds_thenReturnAccountingPointDataNeed() {
        // Given
        var router = new DataNeedRuleSetRouter();
        router.register("id", () -> List.of(new AccountingPointDataNeedRule()));

        // When
        var res = router.supportedDataNeeds();

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(new SupportedDataNeeds("id", Set.of("account")));
    }
}