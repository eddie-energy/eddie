package energy.eddie.core.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
        var dataNeed = dataNeedsService.findById(dataNeedId)
                                       .orElseThrow(() -> new DataNeedNotFoundException(dataNeedId));
        return service.calculate(dataNeed);
    }
}
