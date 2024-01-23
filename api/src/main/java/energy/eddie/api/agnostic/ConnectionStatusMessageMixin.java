package energy.eddie.api.agnostic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.api.v0.DataSourceInformation;

public abstract class ConnectionStatusMessageMixin {
    // Without type information embedded in JSON, explicitly specify the deserialization class for DataSourceInformation
    // @JsonDeserialize(as = DataSourceInformationImpl.class)
    // Or just ignore the field if it is not needed
    @JsonIgnore
    public abstract DataSourceInformation dataSourceInformation();
}