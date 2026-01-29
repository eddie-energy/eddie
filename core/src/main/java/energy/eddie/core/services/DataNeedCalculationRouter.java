// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.exceptions.DataNeedDisabledException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataNeedCalculationRouter {
    private final Map<String, DataNeedCalculationService<DataNeed>> services = new HashMap<>();
    private final DataNeedsService dataNeedsService;

    public DataNeedCalculationRouter(
            @Lazy // the dispatcher servlet is not yet initialized when this bean is created
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // DataNeedsService is a shared component defined in the data-needs module
            DataNeedsService dataNeedsService
    ) {
        this.dataNeedsService = dataNeedsService;
    }

    public void register(DataNeedCalculationService<DataNeed> dataNeedCalculationService) {
        services.put(dataNeedCalculationService.regionConnectorId(), dataNeedCalculationService);
    }

    public DataNeedCalculation calculateFor(
            String regionConnector,
            String dataNeedId
    ) throws UnknownRegionConnectorException, DataNeedNotFoundException {
        var service = services.get(regionConnector);
        if (service == null) {
            throw new UnknownRegionConnectorException(regionConnector);
        }
        return toCalculation(service.calculate(dataNeedId), dataNeedId);
    }

    private DataNeedCalculation toCalculation(
            DataNeedCalculationResult result,
            String dataNeedId
    ) throws DataNeedNotFoundException {
        return switch (result) {
            case DataNeedNotFoundResult ignored -> throw new DataNeedNotFoundException(dataNeedId);
            case DataNeedNotSupportedResult(String message) -> new DataNeedCalculation(false, message);
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe) ->
                    new DataNeedCalculation(true, null, permissionTimeframe, null);
            case ValidatedHistoricalDataDataNeedResult(
                    List<Granularity> granularities,
                    Timeframe permissionTimeframe,
                    Timeframe energyTimeframe
            ) -> new DataNeedCalculation(true, granularities, permissionTimeframe, energyTimeframe);
        };
    }

    public Map<String, DataNeedCalculation> calculate(String dataNeedId) throws DataNeedNotFoundException, DataNeedDisabledException {
        var dataNeed = dataNeedsService.findById(dataNeedId)
                                       .orElseThrow(() -> new DataNeedNotFoundException(dataNeedId));
        if (!dataNeed.isEnabled()) throw new DataNeedDisabledException(dataNeedId);
        var calculations = new HashMap<String, DataNeedCalculation>();
        for (var entry : services.entrySet()) {
            calculations.put(entry.getKey(), toCalculation(entry.getValue().calculate(dataNeed), dataNeedId));
        }
        return calculations;
    }
}
