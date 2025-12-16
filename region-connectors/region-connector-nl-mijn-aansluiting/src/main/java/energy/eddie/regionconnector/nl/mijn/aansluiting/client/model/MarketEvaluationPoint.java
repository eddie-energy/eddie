package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MarketEvaluationPoint(
        @JsonProperty("MRID") String mrid,
        @JsonProperty("RegisterList") List<Register> registerList
) {

    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setRegisterList(@jakarta.annotation.Nonnull List<Register> registerList) {
        this.registerList.clear();
        this.registerList.addAll(registerList);
    }
}

