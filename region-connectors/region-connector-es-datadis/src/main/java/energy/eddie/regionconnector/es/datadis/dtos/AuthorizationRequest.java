package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.regionconnector.es.datadis.serializer.LocalDateToEpochSerializer;

import java.time.LocalDate;
import java.util.List;

/**
 * This class encapsulates the data needed to request authorization from a NIF (User).
 */
public class AuthorizationRequest {
    private static final String MODE = "PULL";

    @JsonProperty("startDatePull")
    @JsonSerialize(using = LocalDateToEpochSerializer.class)
    private LocalDate startDate;

    @JsonProperty("endDatePull")
    @JsonSerialize(using = LocalDateToEpochSerializer.class)
    private LocalDate endDate;

    @JsonProperty("nifSolicitante")
    private String nif;

    @JsonProperty("cups")
    private List<String> meteringPoints;

    public AuthorizationRequest(LocalDate startDate, LocalDate endDate, String nif, List<String> meteringPoints) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.nif = nif;
        this.meteringPoints = meteringPoints;
    }

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

    @JsonProperty("mode")
    public String getMode() {
        return MODE;
    }
}
