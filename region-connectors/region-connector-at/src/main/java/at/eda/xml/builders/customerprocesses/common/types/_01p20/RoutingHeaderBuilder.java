package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.eda.xml.builders.helper.DateTimeConverter;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Objects;

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
        this.sender = Objects.requireNonNull(sender);
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
        this.receiver = Objects.requireNonNull(receiver);
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
        this.documentCreationDateTime = Objects.requireNonNull(documentCreationDateTime);
        return this;
    }

    /**
     * Creates and returns a RoutingHeader Object
     *
     * @return {@link RoutingHeader}
     */
    public RoutingHeader build() {
        RoutingHeader routingAddress = new RoutingHeader();
        routingAddress.setSender(Objects.requireNonNull(sender, "Attribute `sender` is required."));
        routingAddress.setReceiver(Objects.requireNonNull(receiver, "Attribute `receiver` is required."));
        routingAddress.setDocumentCreationDateTime(DateTimeConverter.dateTimeToXml(Objects.requireNonNull(documentCreationDateTime, "Attribute `documentCreationDateTime` is required.")));

        return routingAddress;
    }
}
