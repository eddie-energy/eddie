package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ParamCycType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * <p>Allows to create a ParamCycType Object for CMRequest.
 * <p>All fields are required according to the schema definition.
 *
 * @see ParamCycType
 */
public class ParamCycTypeBuilder {
    @Nullable
    private MeteringIntervallType meteringIntervall;
    @Nullable
    private TransmissionCycle transmissionCycle;

    /**
     * Sets the granularity of the data
     *
     * @param meteringIntervall allowed object is
     *                          {@link MeteringIntervallType}
     * @return {@link ParamCycTypeBuilder}
     */
    public ParamCycTypeBuilder withMeteringIntervall(MeteringIntervallType meteringIntervall) {
        this.meteringIntervall = Objects.requireNonNull(meteringIntervall);
        return this;
    }

    /**
     * Sets the transmission interval consumption data
     *
     * @param transmissionCycle allowed object is
     *                          {@link TransmissionCycle}
     * @return {@link ParamCycTypeBuilder}
     */
    public ParamCycTypeBuilder withTransmissionCycle(TransmissionCycle transmissionCycle) {
        this.transmissionCycle = Objects.requireNonNull(transmissionCycle);
        return this;
    }

    /**
     * Creates and returns a ParamCycType Object
     *
     * @return {@link ParamCycType}
     */
    public ParamCycType build() {
        ParamCycType paramCyc = new ParamCycType();

        paramCyc.setMeteringIntervall(Objects.requireNonNull(meteringIntervall, "Attribute `meteringIntervall` is required."));
        paramCyc.setTransmissionCycle(Objects.requireNonNull(transmissionCycle, "Attribute `transmissionCycle` is required."));

        return paramCyc;
    }
}
