package energy.eddie.core.dtos;

import java.util.Set;

/**
 * DTO used to represent the supported {@code dataNeeds} for a specific {@code regionConnectorId}
 */
public record SupportedDataNeeds(String regionConnectorId, Set<String> dataNeeds) {}