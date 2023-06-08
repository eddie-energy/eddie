package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.utils.TruncatedZonedDateTime;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;


/**
 * A specialized class for ccmo-requests.
 * It allows for the end date to be null, if start lies in the future
 */
public final class CCMOTimeFrame {
    private final ZonedDateTime start;
    @Nullable
    private final ZonedDateTime end;

    public CCMOTimeFrame(ZonedDateTime start, @Nullable ZonedDateTime end) {
        requireNonNull(start);
        start = new TruncatedZonedDateTime(start).zonedDateTime();

        // start lies in the past
        ZonedDateTime now = new TruncatedZonedDateTime(ZonedDateTime
                .now(start.getZone())).zonedDateTime();
        if (start.isBefore(now)) {
            requireNonNull(end);
        }

        if (end != null) {
            end = new TruncatedZonedDateTime(end).zonedDateTime();
            if (!start.isBefore(end) && !start.equals(end)) {
                throw new IllegalArgumentException("End date has to be after/equal start date");
            }
        }
        this.start = start;
        this.end = end;
    }

    public ZonedDateTime start() {
        return start;
    }

    public Optional<ZonedDateTime> end() {
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
