package energy.eddie.api.agnostic;

import javax.annotation.Nullable;

/**
 * Definition of a data need.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataNeedImpl in logical data model</a>
 */
public interface DataNeed {
    String id();

    String description();

    DataType type();

    @Nullable
    Granularity granularity();

    Integer durationStart();

    Boolean durationOpenEnd();

    @Nullable
    Integer durationEnd();
}