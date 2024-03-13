package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import jakarta.persistence.*;

@Entity
@Table(schema = "data_needs")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AiidaDataNeed extends TimeframedDataNeed {
    @Column(name = "transmission_interval", nullable = false)
    @JsonProperty(required = true)
    private int transmissionInterval;

    @SuppressWarnings("NullAway.Init")
    protected AiidaDataNeed() {
    }

    /**
     * Returns the interval in seconds, at which the AIIDA instance should send data.
     */
    public int transmissionInterval() {
        return transmissionInterval;
    }
}
