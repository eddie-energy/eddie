package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import org.junit.jupiter.api.Test;

import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FluviusSupportedDataNeedsSpecificationsTest {
    @Test
    void givenFluviusDataNeedRuleSet_whenDataNeedRuleSet_thenReturnOnlyValidatedHistoricalDataDataNeedsRule() {
        // Given
        var specs = new FluviusDataNeedsRuleSet(false);

        // When
        var res = specs.dataNeedRules();

        // Then
        assertThat(res)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.ELECTRICITY,
                                List.of(Granularity.PT15M, Granularity.P1D),
                                Period.ofYears(-3),
                                Period.ofYears(3)
                        ),
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.NATURAL_GAS,
                                List.of(Granularity.PT30M, Granularity.P1D),
                                Period.ofYears(-3),
                                Period.ofYears(3)
                        )
                );
    }

    @Test
    void givenEnabledSandbox_whenSupportedDataNeeds_thenReturnOnlyValidatedHistoricalDataDataNeedsRule() {
        // Given
        var specs = new FluviusDataNeedsRuleSet(true);

        // When
        var res = specs.dataNeedRules();

        // Then
        assertThat(res)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.ELECTRICITY,
                                List.of(Granularity.PT15M, Granularity.P1D),
                                Period.ofYears(-3),
                                Period.ofYears(3)
                        ),
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.NATURAL_GAS,
                                List.of(Granularity.PT15M, Granularity.P1D),
                                Period.ofYears(-3),
                                Period.ofYears(3)
                        )
                );
    }
}