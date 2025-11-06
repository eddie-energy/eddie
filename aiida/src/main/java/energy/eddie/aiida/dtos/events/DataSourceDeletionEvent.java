package energy.eddie.aiida.dtos.events;

import java.util.List;
import java.util.UUID;

public record DataSourceDeletionEvent(List<UUID> permissionIds) implements AiidaEvent {
}
