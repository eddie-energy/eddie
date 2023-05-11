package at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
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
        ZonedDateTime start = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.of(2022, 1, 2, 0, 0, 0, 0, ZoneId.systemDefault());
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
                ZonedDateTime.now(ZoneId.systemDefault()).plusDays(1),
                ZonedDateTime.now(ZoneId.systemDefault()).plusDays(10)
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
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(10);
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(10);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> type.toString(timeFrame));
    }

    @Test
    void requestDataTypeNowToNow_returnsMeteringData() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(now, now);
        String expectedValue = "MeteringData";

        // when
        String actualValue = type.toString(timeFrame);

        // then
        assertEquals(expectedValue, actualValue);
    }


}