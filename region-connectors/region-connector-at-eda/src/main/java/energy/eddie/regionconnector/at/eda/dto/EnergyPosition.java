package energy.eddie.regionconnector.at.eda.dto;

import java.math.BigDecimal;

public record EnergyPosition(BigDecimal billingQuantity, String meteringMethod) {
}
