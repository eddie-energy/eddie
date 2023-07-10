package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;


import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ProcessDirectory;

import jakarta.annotation.Nullable;
import java.util.Objects;

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
        this.mpDir = Objects.requireNonNull(mpDir);
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
        this.processDir = Objects.requireNonNull(processDir);
        return this;
    }

    /**
     * Creates and returns a CMRequest Object
     *
     * @return {@link CMRequest}
     */
    public CMRequest build() {
        CMRequest cmRequest = new CMRequest();

        cmRequest.setMarketParticipantDirectory(Objects.requireNonNull(mpDir, "Attribute `mpDir` is required."));
        cmRequest.setProcessDirectory(Objects.requireNonNull(processDir, "Attribute `processDir` is required."));

        return cmRequest;
    }
}
