package energy.eddie.outbound.rest.mixins;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public abstract class ConnectionStatusMessageMixin<T> {

    @JsonValue
    public abstract List<T> getConnectionStatusMessage();
}
