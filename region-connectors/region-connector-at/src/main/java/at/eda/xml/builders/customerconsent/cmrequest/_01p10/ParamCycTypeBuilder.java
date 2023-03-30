package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ParamCycType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;

import javax.annotation.Nullable;

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
        if (meteringIntervall == null) {
            throw new IllegalArgumentException("`meteringIntervall` cannot be empty.");
        }

        this.meteringIntervall = meteringIntervall;
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
        if (transmissionCycle == null) {
            throw new IllegalArgumentException("`transmissionCycle` cannot be empty.");
        }

        this.transmissionCycle = transmissionCycle;
        return this;
    }

    /**
     * Creates and returns a ParamCycType Object
     *
     * @return {@link ParamCycType}
     */
    public ParamCycType build() {
        if (meteringIntervall == null || transmissionCycle == null) {
            throw new IllegalStateException("Attributes `meteringIntervall` and `transmissionCycle` are required.");
        }

        ParamCycType paramCyc = new ParamCycType();

        paramCyc.setMeteringIntervall(meteringIntervall);
        paramCyc.setTransmissionCycle(transmissionCycle);

        return paramCyc;
    }
}
