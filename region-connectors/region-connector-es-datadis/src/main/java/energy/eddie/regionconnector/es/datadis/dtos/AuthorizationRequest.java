package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.regionconnector.es.datadis.serializer.LocalDateToEpochSerializer;

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

    @JsonProperty("mode")
    public String getMode() {
        return MODE;
    }
}
