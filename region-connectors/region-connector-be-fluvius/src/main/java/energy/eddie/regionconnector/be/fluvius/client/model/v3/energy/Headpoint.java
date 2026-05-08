// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.api.agnostic.Granularity;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.ELECTRICITY;
import static energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.GAS;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        visible = true,
        property = "$type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MeteringOnHeadpoint.class, name = MeteringOnHeadpoint.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = MeteringOnHeadpointAndMeter.class, name = MeteringOnHeadpointAndMeter.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = MeteringOnMeter.class, name = MeteringOnMeter.DISCRIMINATOR_VALUE),
})
public abstract class Headpoint {
    @JsonProperty(value = "$type", required = true)
    protected final String type;
    @Nullable
    protected final String ean;
    protected final EnergyType energyType;

    protected Headpoint(String type, @Nullable String ean, EnergyType energyType) {
        this.type = type;
        this.ean = ean;
        this.energyType = energyType;
    }

    public abstract boolean dataPresentFor(
            Granularity granularity
    );

    public abstract @Nullable ZonedDateTime getLatestMeterReading(Granularity granularity);

    @Nullable
    public String ean() {
        return ean;
    }

    public boolean isEnergyType(energy.eddie.api.agnostic.data.needs.EnergyType energyType) {
        var requiredType = energyType == energy.eddie.api.agnostic.data.needs.EnergyType.ELECTRICITY ? ELECTRICITY : GAS;
        return this.energyType == requiredType;
    }
}
