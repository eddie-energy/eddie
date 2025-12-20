package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.UnaryOperator;

@Component
public class MeterReadingFilterTask implements UnaryOperator<IdentifiableMeteringData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingFilterTask.class);
    private final DataNeedsService dataNeedsService;

    public MeterReadingFilterTask(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {this.dataNeedsService = dataNeedsService;}

    @Override
    public IdentifiableMeteringData apply(IdentifiableMeteringData meteringData) {
        var pr = meteringData.permissionRequest();
        var permissionId = pr.permissionId();
        var dataNeed = (ValidatedHistoricalDataDataNeed) dataNeedsService.getById(pr.dataNeedId());
        var data = meteringData.payload().data();
        GetEnergyResponseModel newEnergyResponseModel;
        if (dataNeed.energyType() == EnergyType.ELECTRICITY) {
            LOGGER.debug("Electricity data found for permission request {} removing gas data", permissionId);
            newEnergyResponseModel = new GetEnergyResponseModel(data.fetchTime(), List.of(), data.electricityMeters());
        } else {
            LOGGER.debug("Gas data found for permission request {} removing energy data", permissionId);
            newEnergyResponseModel = new GetEnergyResponseModel(data.fetchTime(), data.gasMeters(), List.of());
        }
        return new IdentifiableMeteringData(
                meteringData.permissionRequest(),
                new GetEnergyResponseModelApiDataResponse(
                        meteringData.payload().metaData(),
                        newEnergyResponseModel
                )
        );
    }
}
