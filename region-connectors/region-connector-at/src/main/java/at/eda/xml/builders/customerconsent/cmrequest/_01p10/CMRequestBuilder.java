package at.eda.xml.builders.customerconsent.cmrequest._01p10;


import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ProcessDirectory;

import javax.annotation.Nullable;

/**
 * <p>Allows to create a CMRequest Object.
 * <p>All fields are required according to the schema definition.
 *
 * @see CMRequest
 */
public class CMRequestBuilder {
    @Nullable
    private MarketParticipantDirectory mpDir;
    @Nullable
    private ProcessDirectory processDir;

    /**
     * Sets the basic control data such as sender/receiver, division, message code, etc.
     *
     * @param mpDir allowed object is
     *              {@link MarketParticipantDirectory}
     * @return {@link CMRequestBuilder}
     */
    public CMRequestBuilder withMarketParticipantDirectory(MarketParticipantDirectory mpDir) {
        if (mpDir == null) {
            throw new IllegalArgumentException("`mpDir` cannot be empty.");
        }

        this.mpDir = mpDir;
        return this;
    }

    /**
     * Sets the process relevant data
     *
     * @param processDir allowed object is
     *                   {@link ProcessDirectory}
     * @return {@link CMRequestBuilder}
     */
    public CMRequestBuilder withProcessDirectory(ProcessDirectory processDir) {
        if (processDir == null) {
            throw new IllegalArgumentException("`processDir` cannot be empty.");
        }

        this.processDir = processDir;
        return this;
    }

    /**
     * Creates and returns a CMRequest Object
     *
     * @return {@link CMRequest}
     */
    public CMRequest build() {
        if (mpDir == null || processDir == null) {
            throw new IllegalStateException("Attributes `mpDir` and `processDir` are required.");
        }

        CMRequest cmRequest = new CMRequest();

        cmRequest.setMarketParticipantDirectory(mpDir);
        cmRequest.setProcessDirectory(processDir);

        return cmRequest;
    }
}
