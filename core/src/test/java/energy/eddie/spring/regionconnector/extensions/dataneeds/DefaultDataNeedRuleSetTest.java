package energy.eddie.spring.regionconnector.extensions.dataneeds;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.AiidaDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDataNeedRuleSetTest {
    @Mock
    private RegionConnectorMetadata metadata;

    @Test
    void givenAccountingPointDataNeedSupported_whenSupportedDataNeeds_thenReturnsAccountingPointDataNeedSpecification() {
        // Given
        when(metadata.supportedDataNeeds()).thenReturn(List.of(AccountingPointDataNeed.class));
        var specs = new DefaultDataNeedRuleSet(metadata);

        // When
        var res = specs.dataNeedRules();

        // Then
        assertThat(res)
                .singleElement()
                .isInstanceOf(AccountingPointDataNeedRule.class);
    }

    @ParameterizedTest
    @ValueSource(classes = {InboundAiidaDataNeed.class, OutboundAiidaDataNeed.class})
    void givenAiidaDataNeed_whenSupportedDataNeeds_thenReturnsAiidaDataNeedSpecification(Class<? extends AiidaDataNeed> clazz) {
        // Given
        when(metadata.supportedDataNeeds()).thenReturn(List.of(clazz));
        var specs = new DefaultDataNeedRuleSet(metadata);

        // When
        var res = specs.dataNeedRules();

        // Then
        assertThat(res)
                .singleElement()
                .asInstanceOf(type(AiidaDataNeedRule.class))
                .extracting(AiidaDataNeedRule::getDataNeedClass)
                .isEqualTo(clazz);
    }

    @Test
    void givenValidatedHistoricalDataDataNeedSupported_whenSupportedDataNeeds_thenReturnsValidatedHistoricalDataDataNeedNeedSpecification() {
        // Given
        when(metadata.supportedDataNeeds()).thenReturn(List.of(ValidatedHistoricalDataDataNeed.class));
        when(metadata.granularitiesFor(EnergyType.ELECTRICITY)).thenReturn(List.of(Granularity.P1D));
        when(metadata.supportedEnergyTypes()).thenReturn(List.of(EnergyType.ELECTRICITY));
        when(metadata.earliestStart()).thenReturn(Period.ZERO);
        when(metadata.latestEnd()).thenReturn(Period.ZERO);
        var ruleSet = new DefaultDataNeedRuleSet(metadata);

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .singleElement()
                .asInstanceOf(type(ValidatedHistoricalDataDataNeedRule.class))
                .satisfies(spec -> {
                    assertThat(spec.getDataNeedClass()).isEqualTo(ValidatedHistoricalDataDataNeed.class);
                    assertThat(spec.energyType()).isEqualTo(EnergyType.ELECTRICITY);
                    assertThat(spec.granularities()).isEqualTo(List.of(Granularity.P1D));
                    assertThat(spec.earliestStart()).isEqualTo(Period.ZERO);
                    assertThat(spec.latestEnd()).isEqualTo(Period.ZERO);
                });
    }
}