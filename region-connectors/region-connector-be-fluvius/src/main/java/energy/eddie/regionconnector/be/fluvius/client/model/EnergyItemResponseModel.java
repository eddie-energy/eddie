package energy.eddie.regionconnector.be.fluvius.client.model;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public sealed interface EnergyItemResponseModel<T extends MeasurementResponseModel> permits EDailyEnergyItemResponseModel, EQuarterHourlyEnergyItemResponseModel, GDailyEnergyItemResponseModel, GHourlyEnergyItemResponseModel {
    ZonedDateTime timestampStart();

    ZonedDateTime timestampEnd();

    @Nullable
    List<T> measurement();
}
