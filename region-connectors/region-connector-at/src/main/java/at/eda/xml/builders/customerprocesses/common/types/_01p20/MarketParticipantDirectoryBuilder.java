package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.MarketParticipantDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;

import javax.annotation.Nullable;

/**
 * <p>Allows to create a MarketParticipantDirectory Object (Common Type).
 * <p>Base MarketParticipantDirectory class
 * <p>All fields are required according to the schema definition.
 *
 * @see MarketParticipantDirectory
 * @see at.ebutilities.schemata.customerconsent.cmrequest._01p10.MarketParticipantDirectory
 * @see at.ebutilities.schemata.customerconsent.cmnotification._01p11.MarketParticipantDirectory
 * @see at.ebutilities.schemata.customerprocesses.masterdata._01p30.MarketParticipantDirectory
 * @see at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.MarketParticipantDirectory
 */
public class MarketParticipantDirectoryBuilder {
    @Nullable
    protected RoutingHeader routingHeader;
    protected String sector = "";
    @Nullable
    protected DocumentMode documentMode;
    @Nullable
    protected Boolean duplicate;

    /**
     * Sets the routing header
     *
     * @param routingHeader allowed object is
     *                      {@link RoutingHeader}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withRoutingHeader(RoutingHeader routingHeader) {
        if (routingHeader == null) {
            throw new IllegalArgumentException("`routingHeader` cannot be empty.");
        }

        this.routingHeader = routingHeader;
        return this;
    }

    /**
     * Sets the sector (01 - electricity, 02 - gas)
     *
     * @param sector allowed object is
     *               {@link String}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withSector(String sector) {
        if (sector == null || sector.length() == 0) {
            throw new IllegalArgumentException("`sector` cannot be empty.");
        }

        this.sector = sector;
        return this;
    }

    /**
     * Sets the document mode (indicates whether the content of an XML file is original or merely a simulation)
     *
     * @param documentMode allowed object is
     *                     {@link DocumentMode}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withDocumentMode(DocumentMode documentMode) {
        if (documentMode == null) {
            throw new IllegalArgumentException("`documentMode` cannot be empty.");
        }

        this.documentMode = documentMode;
        return this;
    }

    /**
     * Sets the duplicate label
     *
     * @param duplicate allowed object is
     *                  {@link Boolean}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
        return this;
    }

    /**
     * Creates and returns a MarketParticipantDirectory Object
     *
     * @return {@link MarketParticipantDirectory}
     */
    public MarketParticipantDirectory build() {
        if (routingHeader == null || sector.length() == 0 || documentMode == null || duplicate == null) {
            throw new IllegalStateException("Attributes `routingHeader`, `sector` `documentMode` and `duplicate` are required.");
        }

        MarketParticipantDirectory mpDir = new MarketParticipantDirectory();

        mpDir.setRoutingHeader(routingHeader);
        mpDir.setSector(sector);
        mpDir.setDocumentMode(documentMode);
        mpDir.setDuplicate(duplicate);

        return mpDir;
    }
}
