package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestDataTypeTest {

    @Test
    void requestDataTypeToStringMasterData() {
        // given
        RequestDataType type = RequestDataType.MASTER_DATA;
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.plusDays(1);
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
        LocalDate start = LocalDate.of(2022, 1, 1);
        LocalDate end = LocalDate.of(2022, 1, 2);
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
                LocalDate.now(ZoneOffset.UTC).plusDays(1),
                LocalDate.now(ZoneOffset.UTC).plusDays(10)
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).plusDays(10);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> type.toString(timeFrame));
    }

    @Test
    void requestDataTypeNowToNow_returnsMeteringData() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(now, now);
        String expectedValue = "MeteringData";

        // when
        String actualValue = type.toString(timeFrame);

        // then
        assertEquals(expectedValue, actualValue);
    }
}
