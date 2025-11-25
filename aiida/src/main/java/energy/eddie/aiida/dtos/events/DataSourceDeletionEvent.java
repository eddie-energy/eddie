package energy.eddie.aiida.dtos.events;

import java.util.Set;
import java.util.UUID;

public record DataSourceDeletionEvent(Set<UUID> permissionIds) implements AiidaEvent {
}
