package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotSupportedResult;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class CdsServerCalculationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsServerCalculationService.class);
    private final DataNeedsService dataNeedsService;
    private final DataNeedCalculationService<DataNeed> calculationService;


    public CdsServerCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.dataNeedsService = dataNeedsService;
        this.calculationService = calculationService;
    }

    public DataNeedCalculationResult calculate(
            String dataNeedId,
            CdsServer cdsServer,
            ZonedDateTime referenceDateTime
    ) {
        var calc = calculationService.calculate(dataNeedId, referenceDateTime);
        if (!(calc instanceof ValidatedHistoricalDataDataNeedResult)) {
            return calc;
        }
        var dataNeed = dataNeedsService.getById(dataNeedId);
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed dn)) {
            LOGGER.warn("Mismatch between actual data need {} of type {} and data need calculation {}",
                        dataNeedId,
                        dataNeed.getClass().getSimpleName(),
                        calc);
            return calc;
        }
        if (cdsServer.energyTypes().contains(dn.energyType())) {
            return calc;
        }
        return new DataNeedNotSupportedResult("%s does not support the energy type %s"
                                                      .formatted(cdsServer.name(), dn.energyType()));
    }
}
