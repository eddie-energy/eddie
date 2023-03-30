package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.eda.xml.builders.helper.DateTimeConverter;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

/**
 * <p>Allows to create a RoutingHeader Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see RoutingHeader
 */
public class RoutingHeaderBuilder {
    @Nullable
    private RoutingAddress sender;
    @Nullable
    private RoutingAddress receiver;
    @Nullable
    private LocalDateTime documentCreationDateTime;

    /**
     * Sets the address of the sender
     *
     * @param sender allowed object is
     *               {@link RoutingAddress}
     * @return {@link RoutingHeaderBuilder}
     */
    public RoutingHeaderBuilder withSender(RoutingAddress sender) {
        if (sender == null) {
            throw new IllegalArgumentException("`sender` cannot be empty.");
        }

        this.sender = sender;
        return this;
    }

    /**
     * Sets the address of the receiver
     *
     * @param receiver allowed object is
     *                 {@link RoutingAddress}
     * @return {@link RoutingHeaderBuilder}
     */
    public RoutingHeaderBuilder withReceiver(RoutingAddress receiver) {
        if (receiver == null) {
            throw new IllegalArgumentException("`receiver` cannot be empty.");
        }

        this.receiver = receiver;
        return this;
    }

    /**
     * Sets the creation timestamp of the document
     *
     * @param documentCreationDateTime allowed object is
     *                                 {@link LocalDateTime}
     * @return {@link RoutingHeaderBuilder}
     */
    public RoutingHeaderBuilder withDocCreationDateTime(LocalDateTime documentCreationDateTime) {
        if (documentCreationDateTime == null) {
            throw new IllegalArgumentException("`documentCreationDateTime` cannot be empty.");
        }

        this.documentCreationDateTime = documentCreationDateTime;
        return this;
    }

    /**
     * Creates and returns a RoutingHeader Object
     *
     * @return {@link RoutingHeader}
     */
    public RoutingHeader build() {
        if (sender == null || receiver == null || documentCreationDateTime == null) {
            throw new IllegalStateException("Attribute `sender`, `receiver` and `documentCreationDateTime` are required.");
        }

        RoutingHeader routingAddress = new RoutingHeader();
        routingAddress.setSender(sender);
        routingAddress.setReceiver(receiver);
        routingAddress.setDocumentCreationDateTime(DateTimeConverter.dateTimeToXml(documentCreationDateTime));

        return routingAddress;
    }
}
