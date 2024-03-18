package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.SmartMeterAiidaDataNeed;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AccountingPointDataNeed.class, name = AccountingPointDataNeed.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = ValidatedHistoricalDataDataNeed.class, name = ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = SmartMeterAiidaDataNeed.class, name = SmartMeterAiidaDataNeed.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = GenericAiidaDataNeed.class, name = GenericAiidaDataNeed.DISCRIMINATOR_VALUE)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DataNeed {
    @JsonProperty(required = true)
    private String id;
    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private String description;
    @JsonProperty(required = true)
    private String purpose;
    @JsonProperty(required = true)
    private String policyLink;

    @SuppressWarnings("NullAway.Init")
    protected DataNeed() {
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String purpose() {
        return purpose;
    }

    public String policyLink() {
        return policyLink;
    }

    public void setId(String id) {
        this.id = id;
    }
}
