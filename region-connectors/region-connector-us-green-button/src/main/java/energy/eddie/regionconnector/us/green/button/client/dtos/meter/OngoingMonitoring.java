package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class OngoingMonitoring {
    @JsonProperty(required = true)
    private final String frequency;
    @Nullable
    @JsonProperty(required = true)
    private final ZonedDateTime prepay;
    @Nullable
    @JsonProperty("next_prepay")
    private final ZonedDateTime nextPrepay;
    @Nullable
    @JsonProperty("next_refresh")
    private final ZonedDateTime nextRefresh;
    @Nullable
    @JsonProperty("fixed_refresh_day;")
    private final ZonedDateTime fixedRefreshDay;

    @JsonCreator
    public OngoingMonitoring(
            String frequency,
            @Nullable ZonedDateTime prepay,
            @Nullable ZonedDateTime nextPrepay,
            @Nullable ZonedDateTime nextRefresh,
            @Nullable ZonedDateTime fixedRefreshDay
    ) {
        this.frequency = frequency;
        this.prepay = prepay;
        this.nextPrepay = nextPrepay;
        this.nextRefresh = nextRefresh;
        this.fixedRefreshDay = fixedRefreshDay;
    }
}
