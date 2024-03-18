package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;


/**
 * A data need designed to be fulfilled by an AIIDA instance by sending the data identified by one of the Strings in
 * {@link #dataTags()}.
 */
public class GenericAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "genericAiida";

    @JsonProperty(required = true)
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
