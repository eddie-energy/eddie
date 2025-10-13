package energy.eddie.aiida.dtos.events;

import java.util.UUID;

public record InboundPermissionAcceptEvent(UUID permissionId) implements AiidaEvent {
}
