package at.eda.xml.builders.customerprocesses.common.types._01p20;


import at.ebutilities.schemata.customerprocesses.common.types._01p20.ProcessDirectory;
import at.eda.xml.builders.helper.DateTimeConverter;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Allows to create a ProcessDirectory Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see ProcessDirectory
 */
public class ProcessDirectoryBuilder {
    private String messageId = "";
    private String conversationId = "";
    @Nullable
    private LocalDate processDate;
    private String meteringPoint = "";

    /**
     * Sets the globally unique message ID
     *
     * @param messageId allowed object is
     *                  {@link String} max. length 35
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withMessageId(String messageId) {
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
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withConversationId(String conversationId) {
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
     * Sets the date of the process
     *
     * @param processDate allowed object is
     *                    {@link LocalDate}
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withProcessDate(LocalDate processDate) {
        this.processDate = processDate;
        return this;
    }

    /**
     * Sets the metering point of the participant
     *
     * @param meteringPoint allowed object is
     *                      {@link String} max. length 33
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withMeteringPoint(String meteringPoint) {
        if (meteringPoint == null || meteringPoint.length() == 0) {
            throw new IllegalArgumentException("`meteringPoint` cannot be empty.");
        }

        int LEN_METERING_POINT = 33;
        if (meteringPoint.length() > LEN_METERING_POINT) {
            throw new IllegalArgumentException("`meteringPoint` length cannot exceed " + LEN_METERING_POINT + " characters.");
        }

        String regex = "[0-9A-Za-z]*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(meteringPoint);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("`meteringPoint` does not match the necessary pattern (" + regex + ").");
        }

        this.meteringPoint = meteringPoint;
        return this;
    }

    /**
     * Creates and returns a ProcessDirectory Object
     *
     * @return {@link ProcessDirectory}
     */
    public ProcessDirectory build() {
        if (messageId.length() == 0 || conversationId.length() == 0 || processDate == null || meteringPoint.length() == 0) {
            throw new IllegalStateException("Attributes `messageId`, `conversationId`, `processDate` and `meteringPoint` are required.");
        }

        ProcessDirectory processDir = new ProcessDirectory();

        processDir.setMessageId(messageId);
        processDir.setConversationId(conversationId);
        processDir.setProcessDate(DateTimeConverter.dateToXMl(processDate));
        processDir.setMeteringPoint(meteringPoint);

        return processDir;
    }
}
