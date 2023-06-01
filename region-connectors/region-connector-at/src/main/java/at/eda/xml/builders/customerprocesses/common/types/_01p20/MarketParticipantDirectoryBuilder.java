package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.MarketParticipantDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.eda.xml.builders.helper.Sector;

import jakarta.annotation.Nullable;
import java.util.Objects;

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
    @Nullable
    protected Sector sector;
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
        this.routingHeader = Objects.requireNonNull(routingHeader);
        return this;
    }

    /**
     * Sets the sector (01 - electricity, 02 - gas)
     *
     * @param sector allowed object is
     *               {@link String}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withSector(Sector sector) {
        this.sector = Objects.requireNonNull(sector);
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
        this.documentMode = Objects.requireNonNull(documentMode);
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
        MarketParticipantDirectory mpDir = new MarketParticipantDirectory();

        mpDir.setRoutingHeader(Objects.requireNonNull(routingHeader, "Attribute `routingHeader` is required."));
        mpDir.setSector(Objects.requireNonNull(sector, "Attribute `sector` is required.").value());
        mpDir.setDocumentMode(Objects.requireNonNull(documentMode, "Attribute `documentMode` is required."));
        mpDir.setDuplicate(Objects.requireNonNull(duplicate, "Attribute `duplicate` is required."));

        return mpDir;
    }
}
