package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;

public record IdentifiableMeterReading(
        String permissionId,
        String connectionId,
        String dataNeedId,
        ConsumptionLoadCurveMeterReading payload
) {
}
