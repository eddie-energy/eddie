// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;

public enum DataServiceType {
    QUARTER_HOURLY("VH_kwartier_uur"),
    HALF_HOURLY("VH_half_uur"),
    DAILY("VH_dag");

    private final String value;

    DataServiceType(String value) {
        this.value = value;
    }

    public static DataServiceType from(Granularity granularity) {
        return switch (granularity) {
            case PT15M -> DataServiceType.QUARTER_HOURLY;
            case PT30M -> DataServiceType.HALF_HOURLY;
            case P1D -> DataServiceType.DAILY;
            default -> throw new IllegalArgumentException("Unexpected granularity: " + granularity);
        };
    }

    public static DataServiceType from(FluviusPermissionRequest permissionRequest) {
        return from(permissionRequest.granularity());
    }

    public String value() {
        return value;
    }
}
