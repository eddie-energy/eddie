package energy.eddie.api.agnostic.aiida;

import java.util.UUID;

public record PermissionUpdateDto(PermissionUpdateOperation operation, UUID aiidaId) {}
