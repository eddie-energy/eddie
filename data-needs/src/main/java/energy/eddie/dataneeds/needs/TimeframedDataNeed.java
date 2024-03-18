package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.duration.DataNeedDuration;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


@Entity
@Table(schema = "data_needs")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class TimeframedDataNeed extends DataNeed {
    @JsonProperty(required = true)
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    @Valid
    @NotNull(message = "must not be null")
    private DataNeedDuration duration;

    @SuppressWarnings("NullAway.Init")
    protected TimeframedDataNeed() {
    }

    public DataNeedDuration duration() {
        return duration;
    }
}
