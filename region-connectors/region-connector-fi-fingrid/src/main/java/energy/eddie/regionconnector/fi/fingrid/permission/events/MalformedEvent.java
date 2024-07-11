package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.converters.AttributeErrorListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import java.util.List;

@Entity(name = "FiMalformedEvent")
public class MalformedEvent extends PersistablePermissionEvent {
    @Convert(converter = AttributeErrorListConverter.class)
    @Column(name = "errors", columnDefinition = "text")
    private final List<AttributeError> errors;

    public MalformedEvent(String permissionId, List<AttributeError> errors) {
        super(permissionId, PermissionProcessStatus.MALFORMED);
        this.errors = errors;
    }

    protected MalformedEvent() {
        errors = List.of();
    }

    public List<AttributeError> errors() {
        return errors;
    }
}
