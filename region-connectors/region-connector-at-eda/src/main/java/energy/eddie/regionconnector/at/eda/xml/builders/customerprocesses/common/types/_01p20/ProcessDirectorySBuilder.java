package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.ProcessDirectoryS;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * <p>Allows to create a ProcessDirectoryS Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see ProcessDirectoryS
 * @see at.ebutilities.schemata.customerconsent.cmrequest._01p10.ProcessDirectory
 * @see at.ebutilities.schemata.customerconsent.cmnotification._01p11.ProcessDirectory
 */
public class ProcessDirectorySBuilder {
    private static final int LEN_MESSAGE_ID = 35;
    private static final int LEN_CONVERSATION_ID = 35;
    @Nullable
    protected String messageId;
    @Nullable
    protected String conversationId;

    /**
     * Sets the globally unique message ID
     *
     * @param messageId allowed object is
     *                  {@link String} max. length 35
     * @return {@link ProcessDirectorySBuilder}
     */
    public ProcessDirectorySBuilder withMessageId(String messageId) {
        if (Objects.requireNonNull(messageId).isEmpty()) {
            throw new IllegalArgumentException("`messageId` cannot be empty.");
        }

        if (messageId.length() > LEN_MESSAGE_ID) {
            throw new IllegalArgumentException("`messageId` length cannot exceed " + LEN_MESSAGE_ID + " characters.");
        }

        this.messageId = messageId;
        return this;
    }

    /**
     * Sets the globally unique process ID
     *
     * @param conversationId allowed object is
     *                       {@link String} max. length 35
     * @return {@link ProcessDirectorySBuilder}
     */
    public ProcessDirectorySBuilder withConversationId(String conversationId) {
        if (Objects.requireNonNull(conversationId).isEmpty()) {
            throw new IllegalArgumentException("`conversationId` cannot be empty.");
        }

        if (conversationId.length() > LEN_CONVERSATION_ID) {
            throw new IllegalArgumentException("`conversationId` length cannot exceed " + LEN_CONVERSATION_ID + " characters.");
        }

        this.conversationId = conversationId;
        return this;
    }

    /**
     * Creates and returns a ProcessDirectoryS Object
     *
     * @return {@link ProcessDirectoryS}
     */
    public ProcessDirectoryS build() {
        ProcessDirectoryS processDirS = new ProcessDirectoryS();
        processDirS.setMessageId(Objects.requireNonNull(messageId, "Attributes `messageId` is required."));
        processDirS.setConversationId(Objects.requireNonNull(conversationId, "Attributes `conversationId` is required."));

        return processDirS;
    }
}
