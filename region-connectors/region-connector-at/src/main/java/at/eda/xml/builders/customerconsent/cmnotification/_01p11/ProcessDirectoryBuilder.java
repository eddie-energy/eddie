package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ProcessDirectory;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ResponseDataType;
import at.eda.utils.CMRequestId;
import at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectorySBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * <p>Allows to create a ProcessDirectory Object for CMNotification.
 * <p>All fields are required according to the schema definition.
 *
 * @see ProcessDirectory
 * @see at.ebutilities.schemata.customerprocesses.common.types._01p20.ProcessDirectoryS
 */
public class ProcessDirectoryBuilder extends ProcessDirectorySBuilder {
    private static final int LEN_METERING_POINT = 35;
    @Nullable
    private String cmRequestId;
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
        if (Objects.requireNonNull(cmRequestId).isEmpty()) {
            throw new IllegalArgumentException("`cmRequestId` cannot be empty.");
        }

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
        if (Objects.requireNonNull(responseData).isEmpty()) {
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
        ProcessDirectory processDir = new ProcessDirectory();

        processDir.setMessageId(Objects.requireNonNull(messageId, "Attributes `messageId` is required."));
        processDir.setConversationId(Objects.requireNonNull(conversationId, "Attributes `conversationId` is required."));
        processDir.setCMRequestId(Objects.requireNonNullElseGet(cmRequestId, () -> new CMRequestId(Objects.requireNonNull(messageId)).toString()));
        for (ResponseDataType responseDataEntry : Objects.requireNonNull(responseData, "Attribute `responseData` is required.")) {
            processDir.getResponseData().add(responseDataEntry);
        }

        return processDir;
    }
}
