package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;

import java.time.LocalDate;
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
        LocalDate now = ZonedDateTime.now(EdaRegionConnectorMetadata.AT_ZONE_ID).toLocalDate();
        Optional<ZonedDateTime> end = timeFrame.end();
        if (timeFrame.start().toLocalDate().isBefore(now) && end.isPresent() && end.get().toLocalDate().isAfter(now)) {
            throw new IllegalArgumentException("TimeFrame has to lie completely in the past or completely in the future");
        }
        return timeFrame.start().toLocalDate().isAfter(now) || timeFrame.start().toLocalDate().equals(now)
                ? METERING_DATA_STRING
                : HISTORICAL_METERING_DATA;
    }
}
