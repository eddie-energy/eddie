package energy.eddie.regionconnector.at.eda.dto.masterdata;

import jakarta.annotation.Nullable;

public interface Address {
    @Nullable
    String zipCode();

    @Nullable
    String city();

    @Nullable
    String street();

    @Nullable
    String streetNumber();

    @Nullable
    String staircase();

    @Nullable
    String floor();

    @Nullable
    String door();
}
