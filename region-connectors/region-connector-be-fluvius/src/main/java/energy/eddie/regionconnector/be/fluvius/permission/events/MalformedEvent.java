package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.converters.AttributeErrorListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import java.util.List;

@Entity(name = "BeMalformedEvent")
@SuppressWarnings("unused")
public class MalformedEvent extends PersistablePermissionEvent {
    @Convert(converter = AttributeErrorListConverter.class)
    @Column(name = "errors", columnDefinition = "text")
    private final List<AttributeError> attributeErrors;

    public MalformedEvent(String permissionId, List<AttributeError> attributeErrors) {
        super(permissionId, PermissionProcessStatus.MALFORMED);
        this.attributeErrors = attributeErrors;
    }

    public MalformedEvent(String permissionId, AttributeError attributeErrors) {
        this(permissionId, List.of(attributeErrors));
    }

    protected MalformedEvent() {
        attributeErrors = List.of();
    }
}
