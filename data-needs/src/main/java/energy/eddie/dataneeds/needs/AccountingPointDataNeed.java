package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Data need designed to request accounting point information about a customer from their MDA.
 */
@Entity
@Table(name = "accounting_point_data_need", schema = "data_needs")
@Schema(description = "Data need for accounting point information request, i.e. information about the customer and their metering point.")
public class AccountingPointDataNeed extends DataNeed {
    public static final String DISCRIMINATOR_VALUE = "account";

    @JsonCreator
    public AccountingPointDataNeed() {}

    public AccountingPointDataNeed(
            String name,
            String description,
            String purpose,
            String policyLink,
            boolean enabled,
            @Nullable RegionConnectorFilter regionConnectorFilter
    ) {
        super(name, description, purpose, policyLink, enabled, regionConnectorFilter);
    }
}
