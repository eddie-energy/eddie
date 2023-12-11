package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.api.v0.RegionalInformation;

public abstract class ConnectionStatusMessageMixin {
    @JsonIgnore
    public abstract RegionalInformation regionalInformation();
}