// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record SubHeadpoint(@JsonProperty(value = "$type", required = true) SubHeadpointType subHeadpointType,
                           @Nullable String ean,
                           @Nullable String seqNumber,
                           @Nullable String vregId,
                           @Nullable Type type,
                           @Nullable List<MeasurementSlice> dailyEnergy,
                           @Nullable List<MeasurementSlice> hourlyEnergy,
                           @Nullable List<MeasurementSlice> quarterHourlyEnergy) implements EnergyForGranularity {


    public enum Type {
        @JsonEnumDefaultValue
        @JsonProperty("?")
        UNKNOWN,
        @JsonProperty("NDGR") NDGR,
        @JsonProperty("NDGR2") NDGR2,
        @JsonProperty("PV") PV,
        @JsonProperty("WKKB") WKKB,
        @JsonProperty("WKKD") WKKD,
        @JsonProperty("WKKA") WKKA,
        @JsonProperty("WTR") WTR,
        @JsonProperty("WND") WND,
        @JsonProperty("TBJT") TBJT,
        @JsonProperty("ORC") ORC,
        @JsonProperty("BC") BC,
        @JsonProperty("BIOM") BIOM,
        @JsonProperty("DSL") DSL,
        @JsonProperty("FOSM") FOSM,
        @JsonProperty("PPPV") PPPV,
        @JsonProperty("PPBAT") PPBAT,
        @JsonProperty("BAT") BAT
    }


    public enum SubHeadpointType {
        @JsonEnumDefaultValue
        @JsonProperty("unknown")
        UNKNOWN,
        @JsonProperty("submetering-auxiliary")
        AUXILIARY_SUB_HEADPOINT,
        @JsonProperty("submetering-offtake")
        OFFTAKE_SUB_HEADPOINT,
        @JsonProperty("submetering-production")
        PRODUCTION_SUB_HEADPOINT
    }
}
