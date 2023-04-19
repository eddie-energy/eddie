package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.CMNotification;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ProcessDirectory;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * <p>Allows to create a CMNotification Object.
 * <p>All fields are required according to the schema definition.
 *
 * @see CMNotification
 */
public class CMNotificationBuilder {
    @Nullable
    private MarketParticipantDirectory mpDir;
    @Nullable
    private ProcessDirectory processDir;

    /**
     * Sets the basic control data such as sender/receiver, division, message code, etc.
     *
     * @param mpDir allowed object is
     *              {@link MarketParticipantDirectory}
     * @return {@link CMNotificationBuilder}
     */
    public CMNotificationBuilder withMarketParticipantDirectory(MarketParticipantDirectory mpDir) {
        this.mpDir = Objects.requireNonNull(mpDir);
        return this;
    }

    /**
     * Sets the process relevant data
     *
     * @param processDir allowed object is
     *                   {@link ProcessDirectory}
     * @return {@link CMNotificationBuilder}
     */
    public CMNotificationBuilder withProcessDirectory(ProcessDirectory processDir) {
        this.processDir = Objects.requireNonNull(processDir);
        return this;
    }

    /**
     * Creates and returns a CMNotification Object
     *
     * @return {@link CMNotification}
     */
    public CMNotification build() {
        CMNotification cmNotification = new CMNotification();

        cmNotification.setMarketParticipantDirectory(Objects.requireNonNull(mpDir, "Attribute `mpDir` is required"));
        cmNotification.setProcessDirectory(Objects.requireNonNull(processDir, "Attribute `processDir` is required"));

        return cmNotification;
    }
}
