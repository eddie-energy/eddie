package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.es.datadis.serializer.LocalDateToEpochSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * This class encapsulates the data needed to request authorization from a NIF (User).
 */
public class AuthorizationRequest {
    private static final String MODE = "PULL";
    @JsonProperty("startDatePull")
    @JsonSerialize(using = LocalDateToEpochSerializer.class)
    @SuppressWarnings("unused")
    private LocalDate startDate;
    @JsonProperty("endDatePull")
    @JsonSerialize(using = LocalDateToEpochSerializer.class)
    @SuppressWarnings("unused")
    private LocalDate endDate;
    @JsonProperty("nifSolicitante")
    @SuppressWarnings("unused")
    private String nif;
    @JsonProperty("cups")
    @SuppressWarnings("unused")
    private List<Cups> meteringPoints;

    public AuthorizationRequest(LocalDate startDate, LocalDate endDate, String nif, List<String> meteringPoints) {
        requireNonNull(startDate);
        requireNonNull(endDate);
        requireNonNull(nif);
        requireNonNull(meteringPoints);

        this.startDate = startDate;
        this.endDate = endDate;
        this.nif = nif;
        this.meteringPoints = meteringPoints.stream().map(meteringPoint -> new Cups(meteringPoint, startDate, endDate)).toList();
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    @JsonProperty("mode")
    public String getMode() {
        return MODE;
    }
}
