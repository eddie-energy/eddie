package energy.eddie.core.dataneeds;

import energy.eddie.api.v0.ConsumptionRecord;

import javax.annotation.Nullable;

/**
 * Record defining the attributes of a data need.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataNeed in logical data model</a>
 */
public record DataNeed(
        String id,
        String description,
        DataType type,
        @Nullable ConsumptionRecord.MeteringInterval granularity,
        Integer durationStart,
        Boolean durationOpenEnd,
        @Nullable Integer durationEnd
) {
}
