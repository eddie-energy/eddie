package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity(name = "DeEtaValidatedEvent")
@SuppressWarnings({"unused", "NullAway"})
public class ValidatedEvent extends PersistablePermissionEvent {

    public ValidatedEvent(String permissionId,
                          String connectionId,
                          String dataNeedId,
                          Granularity granularity,
                          LocalDate dataStart,
                          LocalDate dataEnd) {
        super(permissionId,
              PermissionProcessStatus.VALIDATED,
              connectionId,
              dataNeedId,
              dataStart,
              dataEnd,
              granularity == null ? null : granularity.name());
    }

    protected ValidatedEvent() {
        super();
    }
}
