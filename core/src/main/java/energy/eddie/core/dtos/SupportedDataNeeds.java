package energy.eddie.core.dtos;

import java.util.List;

/**
 * DTO used to represent the supported {@code dataNeeds} for a specific {@code regionConnectorId}
 */
public record SupportedDataNeeds(String regionConnectorId, List<String> dataNeeds) {}