package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ProcessDirectory;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ResponseDataType;
import at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectorySBuilder;

import javax.annotation.Nullable;
import java.util.List;

/**
 * <p>Allows to create a ProcessDirectory Object for CMNotification.
 * <p>All fields are required according to the schema definition.
 *
 * @see ProcessDirectory
 * @see at.ebutilities.schemata.customerprocesses.common.types._01p20.ProcessDirectoryS
 */
public class ProcessDirectoryBuilder extends ProcessDirectorySBuilder {
    private String cmRequestId = "";
    @Nullable
    private List<ResponseDataType> responseData;

    /**
     * Sets the globally unique message ID
     *
     * @param messageId allowed object is
     *                  {@link String} max. length 35
     * @return {@link ProcessDirectoryBuilder}
     */
    @Override
    public ProcessDirectoryBuilder withMessageId(String messageId) {
        super.withMessageId(messageId);
        return this;
    }

    /**
     * Sets the globally unique process ID
     *
     * @param conversationId allowed object is
     *                       {@link String} max. length 35
     * @return {@link ProcessDirectoryBuilder}
     */
    @Override
    public ProcessDirectoryBuilder withConversationId(String conversationId) {
        super.withConversationId(conversationId);
        return this;
    }

    /**
     * Sets the globally unique ConsentRequest ID (mapping of a data release request to the end customer)
     *
     * @param cmRequestId allowed object is
     *                    {@link String} max. length 35
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withCMRequestId(String cmRequestId) {
        if (cmRequestId == null || cmRequestId.length() == 0) {
            throw new IllegalArgumentException("`cmRequestId` cannot be empty.");
        }
        int LEN_METERING_POINT = 35;
        if (cmRequestId.length() > LEN_METERING_POINT) {
            throw new IllegalArgumentException("`cmRequestId` length cannot exceed " + LEN_METERING_POINT + " characters.");
        }

        this.cmRequestId = cmRequestId;
        return this;
    }

    /**
     * Sets the response message per request
     *
     * @param responseData allowed object is
     *                     {@link List<ResponseDataType>} max. 33 characters
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withResponseData(List<ResponseDataType> responseData) {
        if (responseData == null || responseData.isEmpty()) {
            throw new IllegalArgumentException("`responseData` cannot be empty.");
        }

        this.responseData = responseData;
        return this;
    }

    /**
     * Creates and returns a ProcessDirectory Object
     *
     * @return {@link ProcessDirectory}
     */
    @Override
    public ProcessDirectory build() {
        super.build();

        if (cmRequestId.length() == 0 || responseData == null) {
            throw new IllegalStateException("Attributes `messageId`, `conversationId`, `cmRequestId` and `responseData` are required.");
        }

        ProcessDirectory processDir = new ProcessDirectory();

        processDir.setMessageId(messageId);
        processDir.setConversationId(conversationId);
        processDir.setCMRequestId(cmRequestId);
        for (ResponseDataType responseDataEntry : responseData) {
            processDir.getResponseData().add(responseDataEntry);
        }

        return processDir;
    }
}
