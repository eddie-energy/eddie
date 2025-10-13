package energy.eddie.aiida.dtos.events;

import java.util.UUID;

public record OutboundPermissionAcceptEvent(UUID permissionId, UUID dataSourceId) implements AiidaEvent {
}
