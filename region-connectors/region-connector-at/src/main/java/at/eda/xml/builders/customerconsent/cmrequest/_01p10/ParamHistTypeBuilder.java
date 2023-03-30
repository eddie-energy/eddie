package at.eda.xml.builders.customerconsent.cmrequest._01p10;


import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ParamHistType;

import javax.annotation.Nullable;

/**
 * <p>Allows to create a ParamHistType Object for CMRequest.
 * <p>All fields are required according to the schema definition.
 *
 * @see ParamHistType
 */
public class ParamHistTypeBuilder {
    @Nullable
    private MeteringIntervallType meteringIntervall;

    /**
     * Sets the granularity of the data
     *
     * @param meteringIntervall allowed object is
     *                          {@link MeteringIntervallType}
     * @return {@link ParamHistTypeBuilder}
     */
    public ParamHistTypeBuilder withMeteringIntervall(MeteringIntervallType meteringIntervall) {
        if (meteringIntervall == null) {
            throw new IllegalArgumentException("`meteringIntervall` cannot be empty.");
        }

        this.meteringIntervall = meteringIntervall;
        return this;
    }

    /**
     * Creates and returns a ParamHistType Object
     *
     * @return {@link ParamHistType}
     */
    public ParamHistType build() {
        if (meteringIntervall == null) {
            throw new IllegalStateException("Attribute `meteringIntervall` is required.");
        }

        ParamHistType paramHist = new ParamHistType();
        paramHist.setMeteringIntervall(meteringIntervall);

        return paramHist;
    }
}
