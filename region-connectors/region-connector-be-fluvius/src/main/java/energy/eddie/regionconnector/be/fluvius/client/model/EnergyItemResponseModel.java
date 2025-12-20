package energy.eddie.regionconnector.be.fluvius.client.model;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public interface EnergyItemResponseModel<T extends MeasurementResponseModel> {
    ZonedDateTime timestampStart();

    ZonedDateTime timestampEnd();

    @Nullable
    List<T> measurement();
}
