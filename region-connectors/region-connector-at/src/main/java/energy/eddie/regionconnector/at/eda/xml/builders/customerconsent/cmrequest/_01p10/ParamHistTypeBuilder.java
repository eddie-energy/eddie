package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;


import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ParamHistType;

import jakarta.annotation.Nullable;
import java.util.Objects;

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
        this.meteringIntervall = Objects.requireNonNull(meteringIntervall);
        return this;
    }

    /**
     * Creates and returns a ParamHistType Object
     *
     * @return {@link ParamHistType}
     */
    public ParamHistType build() {
        ParamHistType paramHist = new ParamHistType();
        paramHist.setMeteringIntervall(Objects.requireNonNull(meteringIntervall, "Attribute `meteringIntervall` is required."));

        return paramHist;
    }
}
