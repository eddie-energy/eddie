package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;

import java.time.LocalDate;
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
        LocalDate now = LocalDate.now(EdaRegionConnectorMetadata.AT_ZONE_ID);
        Optional<LocalDate> end = timeFrame.end();
        if (timeFrame.start().isBefore(now) && end.isPresent() && end.get().isAfter(now)) {
            throw new IllegalArgumentException("TimeFrame has to lie completely in the past or completely in the future");
        }
        return timeFrame.start().isAfter(now) || timeFrame.start().equals(now)
                ? METERING_DATA_STRING
                : HISTORICAL_METERING_DATA;
    }
}
