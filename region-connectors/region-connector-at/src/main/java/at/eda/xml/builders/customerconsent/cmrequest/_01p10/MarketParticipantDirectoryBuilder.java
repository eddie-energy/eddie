package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MarketParticipantDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.eda.xml.builders.helper.Sector;

import javax.annotation.Nullable;
import java.util.Objects;


/**
 * <p>Allows to create a MarketParticipantDirectory Object for CMRequest.
 * <p>All fields are required according to the schema definition.
 *
 * @see MarketParticipantDirectory
 * @see at.ebutilities.schemata.customerprocesses.common.types._01p20.MarketParticipantDirectory
 */
public class MarketParticipantDirectoryBuilder extends at.eda.xml.builders.customerprocesses.common.types._01p20.MarketParticipantDirectoryBuilder {
    private static final int MESSAGE_CODE_LEN = 20;
    @Nullable
    private String messageCode;
    @Nullable
    private String schemaVersion;

    /**
     * Sets the routing header
     *
     * @param routingHeader allowed object is
     *                      {@link RoutingHeader}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    @Override
    public MarketParticipantDirectoryBuilder withRoutingHeader(RoutingHeader routingHeader) {
        super.withRoutingHeader(routingHeader);
        return this;
    }

    /**
     * Sets the sector (01 - electricity, 02 - gas)
     *
     * @param sector allowed object is
     *               {@link String}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    @Override
    public MarketParticipantDirectoryBuilder withSector(Sector sector) {
        super.withSector(sector);
        return this;
    }

    /**
     * Sets the document mode (indicates whether the content of an XML file is original or merely a simulation)
     *
     * @param documentMode allowed object is
     *                     {@link DocumentMode}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    @Override
    public MarketParticipantDirectoryBuilder withDocumentMode(DocumentMode documentMode) {
        super.withDocumentMode(documentMode);
        return this;
    }

    /**
     * Sets the duplicate label
     *
     * @param duplicate allowed object is
     *                  {@link Boolean}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    @Override
    public MarketParticipantDirectoryBuilder withDuplicate(boolean duplicate) {
        super.withDuplicate(duplicate);
        return this;
    }

    /**
     * Sets the message code (ANFORDERUNG_CCMO, ANFORDERUNG_CCMF)
     *
     * @param messageCode allowed object is
     *                    {@link String} max. 20 characters
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withMessageCode(String messageCode) {
        if (Objects.requireNonNull(messageCode).isEmpty()) {
            throw new IllegalArgumentException("`messageCode` cannot be empty.");
        }

        if (messageCode.length() > MESSAGE_CODE_LEN) {
            throw new IllegalArgumentException("`messageCode` max length is ." + MESSAGE_CODE_LEN);
        }

        this.messageCode = messageCode;
        return this;
    }

    /**
     * Sets version of the schema used to create the XML instance
     *
     * @param schemaVersion allowed object is
     *                      {@link String}
     * @return {@link MarketParticipantDirectoryBuilder}
     */
    public MarketParticipantDirectoryBuilder withSchemaVersion(String schemaVersion) {
        if (Objects.requireNonNull(schemaVersion).isEmpty()) {
            throw new IllegalArgumentException("`schemaVersion` cannot be empty.");
        }

        this.schemaVersion = schemaVersion;
        return this;
    }

    /**
     * Creates and returns a MarketParticipantDirectory Object
     *
     * @return {@link MarketParticipantDirectory}
     */
    @Override
    public MarketParticipantDirectory build() {
        MarketParticipantDirectory mpDir = new MarketParticipantDirectory();

        mpDir.setRoutingHeader(Objects.requireNonNull(routingHeader, "Attribute `routingHeader` is required."));
        mpDir.setSector(Objects.requireNonNull(sector, "Attribute `sector` is required.").value());
        mpDir.setDocumentMode(Objects.requireNonNull(documentMode, "Attribute `documentMode` is required."));
        mpDir.setDuplicate(Objects.requireNonNull(duplicate, "Attribute `duplicate` is required."));
        mpDir.setMessageCode(Objects.requireNonNull(messageCode, "Attribute `messageCode` is required."));
        mpDir.setSchemaVersion(Objects.requireNonNull(schemaVersion, "Attribute `schemaVersion` is required."));

        return mpDir;
    }
}
