package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeterBlock {
    @JsonProperty(value = "service_identifier", required = true)
    private final String serviceIdentifier;
    @JsonProperty(value = "service_tariff", required = true)
    private final String serviceTariff;
    @JsonProperty(value = "service_class", required = true)
    private final String serviceClass;
    @JsonProperty(value = "service_address", required = true)
    private final String serviceAddress;
    @JsonProperty(value = "meter_numbers", required = true)
    private final List<String> meterNumbers;
    @JsonProperty(value = "billing_contact", required = true)
    private final String billingContact;
    @JsonProperty(value = "billing_address", required = true)
    private final String billingAddress;
    @JsonProperty(value = "billing_account", required = true)
    private final String billingAccount;

    @SuppressWarnings("java:S107")
    public MeterBlock(
            String serviceIdentifier,
            String serviceTariff,
            String serviceClass,
            String serviceAddress,
            List<String> meterNumbers,
            String billingContact,
            String billingAddress,
            String billingAccount
    ) {
        this.serviceIdentifier = serviceIdentifier;
        this.serviceTariff = serviceTariff;
        this.serviceClass = serviceClass;
        this.serviceAddress = serviceAddress;
        this.meterNumbers = meterNumbers;
        this.billingContact = billingContact;
        this.billingAddress = billingAddress;
        this.billingAccount = billingAccount;
    }

    /**
     * Returns the service class of the meter block. For possible values see the <a
     * href="https://utilityapi.com/docs/api/meters/blocks#service-class-type">documentation</a>.
     *
     * @return the service class of the meter block
     */
    public String serviceClass() {
        return serviceClass;
    }
}

