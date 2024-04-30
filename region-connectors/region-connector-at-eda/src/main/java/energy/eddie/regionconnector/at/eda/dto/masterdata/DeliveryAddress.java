package energy.eddie.regionconnector.at.eda.dto.masterdata;

import jakarta.annotation.Nullable;

public interface DeliveryAddress extends Address {
    @Nullable
    String addressAddition();
}
