// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.rules;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.EnergyCommunityDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;

import java.util.List;

/**
 * This interface is used by the region connectors to specify the supported configurations of data needs.
 */
public sealed interface DataNeedRule {
    /**
     * This interface is used by the region connector to specify supported data need types and its configurations.
     * For example, if a region connector supports the {@link ValidatedHistoricalDataDataNeed} with a {@link Granularity} of {@code PT15M} and the {@link EnergyType} of {@code ELECTRICITY} it could specify it like this:
     * {@code new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT15M));}
     */
    sealed interface SpecificDataNeedRule<T extends DataNeed> extends DataNeedRule {
        /**
         * This method indicates the type of the {@link DataNeed}.
         * It returns the simple class name of the {@link DataNeed}.
         * Primary used to indicate the type for JSON serialization.
         *
         * @return the simple class name of the {@link DataNeed}.
         */
        @JsonProperty("type")
        String getType();

        /**
         * Returns the type of data need that is supported by this rule.
         *
         * @return the type of data need that is supported by this rule.
         */
        Class<T> getDataNeedClass();
    }

    /**
     * The ValidatedHistoricalDataDataNeedRule specifies one supported variant of the {@link ValidatedHistoricalDataDataNeed}.
     *
     * @param energyType    the supported {@link EnergyType}.
     * @param granularities a list of supported {@link Granularity}.
     */
    record ValidatedHistoricalDataDataNeedRule(
            @JsonProperty("energyType") EnergyType energyType,
            @JsonProperty("granularities") List<Granularity> granularities
    ) implements SpecificDataNeedRule<ValidatedHistoricalDataDataNeed> {
        @Override
        public String getType() {
            return ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE;
        }

        @Override
        public Class<ValidatedHistoricalDataDataNeed> getDataNeedClass() {
            return ValidatedHistoricalDataDataNeed.class;
        }
    }

    /**
     * The AccountingPointDataNeedRule specifies one supported variant of the {@link AccountingPointDataNeed}.
     */
    record AccountingPointDataNeedRule() implements SpecificDataNeedRule<AccountingPointDataNeed> {
        @Override
        public String getType() {
            return AccountingPointDataNeed.DISCRIMINATOR_VALUE;
        }

        @Override
        public Class<AccountingPointDataNeed> getDataNeedClass() {
            return AccountingPointDataNeed.class;
        }
    }

    /**
     * The InboundAiidaDataNeedRule specifies one supported variant of the {@link energy.eddie.dataneeds.needs.aiida.AiidaDataNeed}.
     */
    record InboundAiidaDataNeedRule() implements SpecificDataNeedRule<InboundAiidaDataNeed> {
        @Override
        public String getType() {
            return InboundAiidaDataNeed.DISCRIMINATOR_VALUE;
        }

        @Override
        public Class<InboundAiidaDataNeed> getDataNeedClass() {
            return InboundAiidaDataNeed.class;
        }
    }

    /**
     * The OutboundAiidaDataNeedRule specifies one supported variant of the {@link energy.eddie.dataneeds.needs.aiida.AiidaDataNeed}.
     */
    record OutboundAiidaDataNeedRule() implements SpecificDataNeedRule<OutboundAiidaDataNeed> {
        @Override
        public String getType() {
            return OutboundAiidaDataNeed.DISCRIMINATOR_VALUE;
        }

        @Override
        public Class<OutboundAiidaDataNeed> getDataNeedClass() {
            return OutboundAiidaDataNeed.class;
        }
    }

    /**
     * The EnergyCommunityDataNeedRule specifies that the region connector supports {@link EnergyCommunityDataNeed}.
     */
    record EnergyCommunityDataNeedRule() implements SpecificDataNeedRule<EnergyCommunityDataNeed> {
        @Override
        public String getType() {
            return EnergyCommunityDataNeed.DISCRIMINATOR_VALUE;
        }

        @Override
        public Class<EnergyCommunityDataNeed> getDataNeedClass() {
            return EnergyCommunityDataNeed.class;
        }
    }

    /**
     * If this rule is present in a {@link DataNeedRuleSet} the region connector supports multiple data needs for creating permission requests.
     * How the region connector supports this is not specified and up to the specific region connector.
     */
    record AllowMultipleDataNeedsRule() implements DataNeedRule {
        @JsonProperty
        public boolean allowMultipleDataNeeds() {
            return true;
        }
    }
}
