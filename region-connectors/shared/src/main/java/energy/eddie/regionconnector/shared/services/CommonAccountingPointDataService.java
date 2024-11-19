package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

public interface CommonAccountingPointDataService {
    void fetchAccountingPointData(MeterReadingPermissionRequest request, String usagePointId);
}
