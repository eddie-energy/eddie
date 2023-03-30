package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MarketParticipantDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;


/**
 * <p>Allows to create a MarketParticipantDirectory Object for CMRequest.
 * <p>All fields are required according to the schema definition.
 *
 * @see MarketParticipantDirectory
 * @see at.ebutilities.schemata.customerprocesses.common.types._01p20.MarketParticipantDirectory
 */
public class MarketParticipantDirectoryBuilder extends at.eda.xml.builders.customerprocesses.common.types._01p20.MarketParticipantDirectoryBuilder {
    private String messageCode = "";
    private String schemaVersion = "";

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
    public MarketParticipantDirectoryBuilder withSector(String sector) {
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
        if (messageCode == null || messageCode.length() == 0) {
            throw new IllegalArgumentException("`messageCode` cannot be empty.");
        }

        int MESSAGE_CODE_LEN = 20;
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
        if (schemaVersion == null || schemaVersion.length() == 0) {
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
        super.build();
        if (messageCode.length() == 0 || schemaVersion.length() == 0) {
            throw new IllegalStateException("Attributes `messageCode`, `schemaVersion`, `routingHeader`, `sector`," +
                    "`documentMode` and `duplicate` are required.");
        }

        MarketParticipantDirectory mpDir = new MarketParticipantDirectory();
        mpDir.setRoutingHeader(routingHeader);
        mpDir.setSector(sector);
        mpDir.setDocumentMode(documentMode);
        mpDir.setDuplicate(duplicate);
        mpDir.setMessageCode(messageCode);
        mpDir.setSchemaVersion(schemaVersion);

        return mpDir;
    }
}
