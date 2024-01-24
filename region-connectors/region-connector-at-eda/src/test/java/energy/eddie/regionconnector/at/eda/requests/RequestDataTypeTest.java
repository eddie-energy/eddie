package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestDataTypeTest {

    @Test
    void requestDataTypeToStringMasterData() {
        // given
        RequestDataType type = RequestDataType.MASTER_DATA;
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        String expectedValue = "MasterData";

        // when
        String actualValue = type.toString(timeFrame);

        // then
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void requestDataTypeToStringMeteringDataPast() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        ZonedDateTime start = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.of(2022, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        String expectedValue = "HistoricalMeteringData";

        // when
        String actualValue = type.toString(timeFrame);

        // then
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void requestDataTypeToStringMeteringDataFuture() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(1),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(10)
        );
        String expectedValue = "MeteringData";

        // when
        String actualValue = type.toString(timeFrame);

        // then
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void requestDataTypeToStringMixed() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> type.toString(timeFrame));
    }

    @Test
    void requestDataTypeNowToNow_returnsMeteringData() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(now, now);
        String expectedValue = "MeteringData";

        // when
        String actualValue = type.toString(timeFrame);

        // then
        assertEquals(expectedValue, actualValue);
    }


}