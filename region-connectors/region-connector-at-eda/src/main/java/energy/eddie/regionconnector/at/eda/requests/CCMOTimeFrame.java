package energy.eddie.regionconnector.at.eda.requests;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;


/**
 * A specialized class for ccmo-requests.
 * It allows for the end date to be null
 */
public final class CCMOTimeFrame {
    private final ZonedDateTime start;
    @Nullable
    private final ZonedDateTime end;

    public CCMOTimeFrame(ZonedDateTime start, @Nullable ZonedDateTime end) {
        requireNonNull(start);
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
