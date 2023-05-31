package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.utils.TruncatedZonedDateTime;

import java.time.ZonedDateTime;
import java.util.Optional;

public enum RequestDataType {
    MASTER_DATA,
    METERING_DATA;

    private static final String METERING_DATA_STRING = "MeteringData";
    private static final String HISTORICAL_METERING_DATA = "HistoricalMeteringData";
    private static final String MASTER_DATA_STRING = "MasterData";

    public String toString(CCMOTimeFrame timeFrame) {
        if (this.equals(MASTER_DATA)) {
            return MASTER_DATA_STRING;
        }
        ZonedDateTime now = new TruncatedZonedDateTime(ZonedDateTime.now(timeFrame.start().getZone()))
                .zonedDateTime();
        Optional<ZonedDateTime> end = timeFrame.end();
        if (timeFrame.start().isBefore(now) && end.isPresent() && end.get().isAfter(now)) {
            throw new IllegalArgumentException("TimeFrame has to lie completely in the past or completely in the future");
        }
        return timeFrame.start().isAfter(now) || timeFrame.start().equals(now) ? METERING_DATA_STRING : HISTORICAL_METERING_DATA;
    }
}
