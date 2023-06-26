package energy.eddie.regionconnector.at.eda.requests;

import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;


/**
 * A specialized class for ccmo-requests.
 * It allows for the end date to be null, if start lies in the future
 */
public final class CCMOTimeFrame {
    private final LocalDate start;
    @Nullable
    private final LocalDate end;

    public CCMOTimeFrame(LocalDate start, @Nullable LocalDate end) {
        requireNonNull(start);

        // start lies in the past
        LocalDate now = LocalDate.now();
        if (start.isBefore(now)) {
            requireNonNull(end);
        }

        if (end != null) {
            if (!start.isBefore(end) && !start.equals(end)) {
                throw new IllegalArgumentException("End date has to be after/equal start date");
            }
        }
        this.start = start;
        this.end = end;
    }

    public LocalDate start() {
        return start;
    }

    public Optional<LocalDate> end() {
        return Optional.ofNullable(end);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CCMOTimeFrame) obj;
        return Objects.equals(this.start, that.start) &&
                Objects.equals(this.end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "CCMOTimeFrame[" +
                "start=" + start + ", " +
                "end=" + end + ']';
    }

}
