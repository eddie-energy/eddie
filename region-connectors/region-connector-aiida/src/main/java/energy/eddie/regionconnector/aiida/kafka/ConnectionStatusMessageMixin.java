package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.api.v0.DataSourceInformation;

public abstract class ConnectionStatusMessageMixin {
    @JsonIgnore
    public abstract DataSourceInformation dataSourceInformation();
}