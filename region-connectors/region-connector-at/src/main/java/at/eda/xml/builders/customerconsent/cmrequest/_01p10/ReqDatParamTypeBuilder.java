package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ParamCycType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ParamHistType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ReqDatParamType;

import javax.annotation.Nullable;


/**
 * <p>Allows to create a ProcessDirectory Object for CMRequest.
 * <p>Either <b>paramCyc</b> or <b>paramHist</b> are required but not BOTH according to the schema definition.
 *
 * @see ReqDatParamType
 */
public class ReqDatParamTypeBuilder {
    @Nullable
    private ParamCycType paramCyc;
    @Nullable
    private ParamHistType paramHist;

    public ReqDatParamTypeBuilder withParamCyc(ParamCycType paramCyc) {
        this.paramCyc = paramCyc;
        return this;
    }

    /**
     * Sets available historical periods with granularity
     *
     * @param paramHist allowed object is
     *                  {@link ParamHistType}
     * @return {@link ReqDatParamTypeBuilder}
     */
    public ReqDatParamTypeBuilder withParamHist(ParamHistType paramHist) {
        this.paramHist = paramHist;
        return this;
    }

    /**
     * Creates and returns a ReqDatParamType Object
     *
     * @return {@link ReqDatParamType}
     */
    public ReqDatParamType build() {
        if (paramCyc != null && paramHist != null) {
            throw new IllegalStateException("Either `paramCyc` or `paramHist` is allowed but not both.");
        }
        if (paramCyc == null && paramHist == null) {
            throw new IllegalStateException("Either `paramCyc` or `paramHist` is required.");
        }

        ReqDatParamType reqDatParam = new ReqDatParamType();

        reqDatParam.setParamCyc(paramCyc);
        reqDatParam.setParamHist(paramHist);

        return reqDatParam;
    }
}
