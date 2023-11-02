package energy.eddie.regionconnector.at.eda.requests;

import jakarta.annotation.Nullable;

import java.util.Optional;

public class DsoIdAndMeteringPoint {
    public static final int DSO_ID_LENGTH = 8;

    @Nullable
    private final String meteringPoint;

    @Nullable
    private final String dsoId;

    public DsoIdAndMeteringPoint(@Nullable String dsoId, @Nullable String meteringPoint) {
        this.dsoId = dsoId;
        this.meteringPoint = meteringPoint;
    }

    public String dsoId() throws InvalidDsoIdException {
        if (dsoId != null && !dsoId.isBlank()) {
            return dsoId;
        }
        return meteringPoint()
                .orElseThrow(() -> new InvalidDsoIdException("Either DSO-ID or metering point must not be null"))
                .substring(0, DSO_ID_LENGTH);
    }

    public Optional<String> meteringPoint() {
        return Optional.ofNullable(meteringPoint);
    }
}
