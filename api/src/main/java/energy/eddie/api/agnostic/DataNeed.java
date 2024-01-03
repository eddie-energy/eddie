package energy.eddie.api.agnostic;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Definition of a data need.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataNeedImpl in logical data model</a>
 */
public interface DataNeed {
    String id();

    String description();

    DataType type();

    Granularity granularity();

    Integer durationStart();

    Boolean durationOpenEnd();

    @Nullable
    Integer durationEnd();

    /**
     * Specifies the amount of seconds that should elapse between two data transmissions from an AIIDA instance to the EP.
     * <br>
     * Only available if {@link #type()} is {@code AIIDA_NEAR_REALTIME_DATA}.
     *
     * @return Transmission interval in seconds
     */
    @Nullable
    Integer transmissionInterval();

    /**
     * Specifies the IDs of the data that should be shared.
     * <br>
     * Only available if {@link #type()} is {@code AIIDA_NEAR_REALTIME_DATA}.
     *
     * @return Set of Strings that represent data IDs.
     */
    @Nullable
    Set<String> sharedDataIds();

    /**
     * Name of the service for which this DataNeed describes the required data.
     * <br>
     * Only available if {@link #type()} is {@code AIIDA_NEAR_REALTIME_DATA}.
     *
     * @return Name of the service.
     */
    @Nullable
    String serviceName();
}