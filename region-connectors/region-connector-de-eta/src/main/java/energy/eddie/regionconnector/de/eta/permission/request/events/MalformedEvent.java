package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.converters.AttributeErrorListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Event emitted when a permission request is malformed (validation failed).
 */
@Entity(name = "DeMalformedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class MalformedEvent extends PersistablePermissionEvent {
    @Convert(converter = AttributeErrorListConverter.class)
    @Column(name = "errors", columnDefinition = "text")
    private List<AttributeError> attributeErrors;

    public MalformedEvent(String permissionId, List<AttributeError> attributeErrors) {
        super(permissionId, PermissionProcessStatus.MALFORMED);
        this.attributeErrors = attributeErrors;
    }

    public MalformedEvent(String permissionId, AttributeError attributeError) {
        this(permissionId, List.of(attributeError));
    }

    protected MalformedEvent() { }

    public List<AttributeError> attributeErrors() {
        return attributeErrors;
    }
}
