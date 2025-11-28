package energy.eddie.regionconnector.de.eta.permission.requests;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class DateRange {
    private LocalDate start;
    private LocalDate end;

    protected DateRange() {
        // for JPA
    }

    public DateRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }
}
