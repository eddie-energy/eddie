package energy.eddie.aiida.dtos.events;

import java.util.UUID;

public record InboundPermissionRevokeEvent(UUID dataSourceId) implements AiidaEvent {
}
