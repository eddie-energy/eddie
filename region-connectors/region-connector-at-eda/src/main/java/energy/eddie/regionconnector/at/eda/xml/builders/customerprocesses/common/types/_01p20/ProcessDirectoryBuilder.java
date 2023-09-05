package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;


import at.ebutilities.schemata.customerprocesses.common.types._01p20.ProcessDirectory;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.DateTimeConverter;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * <p>Allows to create a ProcessDirectory Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see ProcessDirectory
 */
public class ProcessDirectoryBuilder {
    private static final int LEN_MESSAGE_ID = 35;
    private static final int LEN_CONVERSATION_ID = 35;
    private static final int LEN_METERING_POINT = 33;
    @Nullable
    private String messageId;
    @Nullable
    private String conversationId;
    @Nullable
    private LocalDate processDate;
    @Nullable
    private String meteringPoint;

    /**
     * Sets the globally unique message ID
     *
     * @param messageId allowed object is
     *                  {@link String} max. length 35
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withMessageId(String messageId) {
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
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withConversationId(String conversationId) {
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
     * Sets the date of the process
     *
     * @param processDate allowed object is
     *                    {@link LocalDate}
     * @return {@link ProcessDirectoryBuilder}
     */
    public ProcessDirectoryBuilder withProcessDate(LocalDate processDate) {
        this.processDate = Objects.requireNonNull(processDate);
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
        if (Objects.requireNonNull(meteringPoint).isEmpty()) {
            throw new IllegalArgumentException("`meteringPoint` cannot be empty.");
        }

        if (meteringPoint.length() > LEN_METERING_POINT) {
            throw new IllegalArgumentException("`meteringPoint` length cannot exceed " + LEN_METERING_POINT + " characters.");
        }

        for (int i = 0; i < meteringPoint.length(); i++) {
            char ch = meteringPoint.charAt(i);
            if (!Character.isLetterOrDigit(ch)) {
                throw new IllegalArgumentException("`meteringPoint` must consist only of letters and digits.");
            }
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
        ProcessDirectory processDir = new ProcessDirectory();

        processDir.setMessageId(Objects.requireNonNull(messageId, "Attribute `messageId` is required."));
        processDir.setConversationId(Objects.requireNonNull(conversationId, "Attribute `conversationId` is required."));
        processDir.setProcessDate(DateTimeConverter.dateToXml(Objects.requireNonNullElseGet(processDate, LocalDate::now)));
        processDir.setMeteringPoint(Objects.requireNonNull(meteringPoint, "Attribute `meteringPoint` is required."));

        return processDir;
    }
}
