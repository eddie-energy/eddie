package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the data needed to request authorization from a NIF (User).
 */
public class AuthorizationRequest {
    @JsonProperty("mode")
    private static final String MODE = "PULL";

    @JsonProperty("startDatePull")
    private LocalDate startDate;

    @JsonProperty("endDatePull")
    private LocalDate endDate;

    @JsonProperty("nifSolicitante")
    private String nif;

    @JsonProperty("cups")
    private List<String> meteringPoints = new ArrayList<>();

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public List<String> getMeteringPoints() {
        return meteringPoints;
    }

    public void setMeteringPoints(List<String> meteringPoints) {
        this.meteringPoints = meteringPoints;
    }
}
