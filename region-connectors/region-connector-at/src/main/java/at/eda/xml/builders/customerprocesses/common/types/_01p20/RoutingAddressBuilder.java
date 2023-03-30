package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Allows to create a RoutingAddress Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see RoutingAddress
 */
public class RoutingAddressBuilder {
    private String messageAddress = "";
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
        if (messageAddress == null || messageAddress.length() == 0) {
            throw new IllegalArgumentException("`messageAddress` cannot be empty.");
        }

        String regex = "[A-Za-z]{2}[0-9]{6}";
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
        if (addressType == null) {
            throw new IllegalArgumentException("`addressType` cannot be empty.");
        }

        this.addressType = addressType;
        return this;
    }

    /**
     * Creates and returns a RoutingAddress Object
     *
     * @return {@link RoutingAddress}
     */
    public RoutingAddress build() {
        if (messageAddress.length() == 0 || addressType == null) {
            throw new IllegalStateException("Attributes `messageAddress` and `addressType` are required.");
        }

        RoutingAddress routingAddress = new RoutingAddress();
        routingAddress.setMessageAddress(messageAddress);
        routingAddress.setAddressType(addressType);

        return routingAddress;
    }
}
