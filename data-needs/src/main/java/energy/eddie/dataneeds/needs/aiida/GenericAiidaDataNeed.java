package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;


/**
 * A data need designed to be fulfilled by an AIIDA instance by sending the data identified by one of the Strings in
 * {@link #dataTags()}.
 */
@Entity
@Table(name = "generic_aiida_data_need", schema = "data_needs")
public class GenericAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "genericAiida";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "generic_aiida_data_need_data_tags",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "data_tags")
    @JsonProperty(required = true)
    @NotEmpty(message = "must contain at least one data tag")
    private Set<String> dataTags;

    @SuppressWarnings("NullAway.Init")
    protected GenericAiidaDataNeed() {
    }

    /**
     * Returns the set of identifiers for the data that should be shared by the AIIDA instance.
     */
    public Set<String> dataTags() {
        return dataTags;
    }
}
