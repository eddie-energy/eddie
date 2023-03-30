package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.ProcessDirectoryS;

/**
 * <p>Allows to create a ProcessDirectoryS Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see ProcessDirectoryS
 * @see at.ebutilities.schemata.customerconsent.cmrequest._01p10.ProcessDirectory
 * @see at.ebutilities.schemata.customerconsent.cmnotification._01p11.ProcessDirectory
 */
public class ProcessDirectorySBuilder {
    protected String messageId = "";
    protected String conversationId = "";

    /**
     * Sets the globally unique message ID
     *
     * @param messageId allowed object is
     *                  {@link String} max. length 35
     * @return {@link ProcessDirectorySBuilder}
     */
    public ProcessDirectorySBuilder withMessageId(String messageId) {
        if (messageId == null || messageId.length() == 0) {
            throw new IllegalArgumentException("`messageId` cannot be empty.");
        }

        int LEN_MESSAGE_ID = 35;
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
        if (conversationId == null || conversationId.length() == 0) {
            throw new IllegalArgumentException("`conversationId` cannot be empty.");
        }

        int LEN_CONVERSATION_ID = 35;
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
        if (messageId.length() == 0 || conversationId.length() == 0) {
            throw new IllegalStateException("Attributes `messageId` and `conversationId` are required.");
        }

        ProcessDirectoryS processDirS = new ProcessDirectoryS();
        processDirS.setMessageId(messageId);
        processDirS.setConversationId(conversationId);

        return processDirS;
    }
}
