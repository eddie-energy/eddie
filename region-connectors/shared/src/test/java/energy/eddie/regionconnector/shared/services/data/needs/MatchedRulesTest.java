package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.MatchedVHDRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchedRulesTest {

    @Test
    void givenValidatedHistoricalDataDataNeed_whenForEnergyType_thenReturnRule() {
        // Given
        var expected = new ValidatedHistoricalDataDataNeedRule(
                EnergyType.NATURAL_GAS,
                List.of(Granularity.PT15M)
        );
        var rules = new MatchedVHDRules(
                List.of(
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.ELECTRICITY,
                                List.of(Granularity.P1D)
                        ),
                        expected
                ),
                new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.PT15M,
                        Granularity.P1D
                )
        );

        // When
        var res = rules.forEnergyType();

        // Then
        assertThat(res)
                .isPresent()
                .hasValue(expected);
    }
}