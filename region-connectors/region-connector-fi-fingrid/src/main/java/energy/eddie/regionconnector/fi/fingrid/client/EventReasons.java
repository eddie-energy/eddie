package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EventReasons(@JsonProperty("EventReason") List<EventReason> reasons) {
}
