package energy.eddie.dataneeds.rules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;

import java.util.List;

/**
 * This interface is used by the region connectors to specify the exact configuration of a single data need they support.
 * For example, if a region connector supports the {@link ValidatedHistoricalDataDataNeed} with a {@link Granularity} of {@code PT15M} and the {@link EnergyType} of {@code ELECTRICITY} it could specify it like this:
 * {@code new SupportedValidatedHistoricalDataDataNeedSpecification(List.of(PT15M), List.of(Granularity.PT15M));}
 *
 * @param <T> the type of data need that is supported.
 */
public sealed interface DataNeedRule<T extends DataNeed> {
    /**
     * The {@link DataNeed} class of the DataNeedRule.
     * @return the class of the {@link DataNeed} that is supported.
     */
    @JsonIgnore
    Class<T> getDataNeedClass();

    /**
     * This method indicates the type of the {@link DataNeed}.
     * It returns the simple class name of the {@link DataNeed}.
     * Primary used to indicate the type for JSON serialization.
     *
     * @return the simple class name of the {@link DataNeed}.
     */
    @JsonProperty("type")
    default String getType() {
        return getDataNeedClass().getSimpleName();
    }

    /**
     * The SupportedValidatedHistoricalDataDataNeedSpecification specifies one supported variant of the {@link ValidatedHistoricalDataDataNeed}.
     * @param energyType    the supported {@link EnergyType}.
     * @param granularities a list of supported {@link Granularity}.
     */
    record ValidatedHistoricalDataDataNeedRule(
            @JsonProperty("energyTypes") EnergyType energyType,
            @JsonProperty("granularities") List<Granularity> granularities
    ) implements DataNeedRule<ValidatedHistoricalDataDataNeed> {
        @Override
        public Class<ValidatedHistoricalDataDataNeed> getDataNeedClass() {
            return ValidatedHistoricalDataDataNeed.class;
        }
    }

    /**
     * The SupportedAccountingPointDataNeedSpecification specifies one supported variant of the {@link AccountingPointDataNeed}.
     */
    record AccountingPointDataNeedRule() implements DataNeedRule<AccountingPointDataNeed> {
        @Override
        public Class<AccountingPointDataNeed> getDataNeedClass() {
            return AccountingPointDataNeed.class;
        }
    }

    /**
     * The SupportedAiidaDataNeedSpecification specifies one supported variant of either the {@link energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed} or the {@link energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed}.
     * @param aiidaDataNeedClass specifies which of the {@link AiidaDataNeed} is supported.
     * @param <T>                The concrete {@link AiidaDataNeed}.
     */
    record AiidaDataNeedRule<T extends AiidaDataNeed>(Class<T> aiidaDataNeedClass)
            implements DataNeedRule<T> {
        @Override
        public Class<T> getDataNeedClass() {
            return aiidaDataNeedClass;
        }
    }
}
