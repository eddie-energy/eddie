package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;

import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Allows to create a RoutingAddress Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see RoutingAddress
 */
public class RoutingAddressBuilder {
    @Nullable
    private String messageAddress;
    @Nullable
    private AddressType addressType;

    /**
     * Sets the address of the sender/receiver
     *
     * @param messageAddress allowed object is
     *                       {@link String} needs to match regex {@code [A-Za-z]{2}[0-9]{6}}
     * @return {@link RoutingHeaderBuilder}
     */
    public RoutingAddressBuilder withMessageAddress(String messageAddress) {
        if (Objects.requireNonNull(messageAddress).isEmpty()) {
            throw new IllegalArgumentException("`messageAddress` cannot be empty.");
        }

        String regex = "[A-Za-z]{2}\\d{6}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(messageAddress);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("`messageAddress` does not match the necessary pattern (" + regex + ").");
        }

        this.messageAddress = messageAddress;
        return this;
    }

    /**
     * Sets the address type of the sender/receiver
     *
     * @param addressType allowed object is
     *                    {@link AddressType}
     * @return {@link RoutingHeaderBuilder}
     */
    public RoutingAddressBuilder withAddressType(AddressType addressType) {
        this.addressType = Objects.requireNonNull(addressType);
        return this;
    }

    /**
     * Creates and returns a RoutingAddress Object
     *
     * @return {@link RoutingAddress}
     */
    public RoutingAddress build() {
        RoutingAddress routingAddress = new RoutingAddress();
        routingAddress.setMessageAddress(Objects.requireNonNull(messageAddress, "Attribute `messageAddress` is required."));
        routingAddress.setAddressType(Objects.requireNonNull(addressType, "Attribute `addressType` is required."));

        return routingAddress;
    }
}
