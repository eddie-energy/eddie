package energy.eddie.dataneeds.needs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Data need designed to request accounting point information about a customer from their MDA.
 */
@Entity
@Table(name = "accounting_point_data_need", schema = "data_needs")
public class AccountingPointDataNeed extends DataNeed {
    public static final String DISCRIMINATOR_VALUE = "account";
}
