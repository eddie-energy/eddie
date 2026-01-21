package energy.eddie.core.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.CalculationResult;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.InvalidDataNeedCombination;
import energy.eddie.core.dtos.MultipleDataNeedsOrErrorResult;
import energy.eddie.dataneeds.exceptions.DataNeedDisabledException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static energy.eddie.core.dtos.MultipleDataNeedsOrErrorResult.MultipleDataNeeds;
import static energy.eddie.core.dtos.MultipleDataNeedsOrErrorResult.MultipleDataNeedsError;

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

    public MultipleDataNeedsOrErrorResult calculateFor(
            String regionConnector,
            Set<String> dataNeedIds
    ) throws UnknownRegionConnectorException {
        var service = services.get(regionConnector);
        if (service == null) {
            throw new UnknownRegionConnectorException(regionConnector);
        }
        return switch (service.calculateAll(dataNeedIds)) {
            case CalculationResult(Map<String, DataNeedCalculationResult> result) -> {
                var m = new HashMap<String, DataNeedCalculation>();
                for (var entry : result.entrySet()) {
                    try {
                        m.put(entry.getKey(), toCalculation(entry.getValue(), entry.getKey()));
                    } catch (DataNeedNotFoundException e) {
                        m.put(entry.getKey(), null);
                    }
                }
                yield new MultipleDataNeeds(m);
            }
            case InvalidDataNeedCombination(Set<String> offendingDataNeedIds, String message) ->
                    new MultipleDataNeedsError(offendingDataNeedIds, message);
        };
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
}
