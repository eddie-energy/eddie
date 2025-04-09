package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p21.CMRequest01p21OutboundMessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestDataTypeTest {

    public static Stream<Arguments> variousDateRanges() {
        LocalDate now = LocalDate.now(EdaRegionConnectorMetadata.AT_ZONE_ID);
        return Stream.of(
                Arguments.of(now.minusWeeks(2), now.minusWeeks(1), "completely in the past"),
                Arguments.of(now.minusWeeks(2), now.plusWeeks(1), "past to future"),
                Arguments.of(now.plusWeeks(1), now.plusWeeks(2), "completely in the future")
        );
    }

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

    @ParameterizedTest(name = "{0} {1}: {2}")
    @MethodSource("variousDateRanges")
    void testToString_AfterActiveFromDate(LocalDate from, LocalDate to, String ignoredMessage) {
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(from, to);
        Clock clock = Clock.fixed(
                CMRequest01p21OutboundMessageFactory.ACTIVE_FROM
                        .atStartOfDay(EdaRegionConnectorMetadata.AT_ZONE_ID)
                        .toInstant(),
                EdaRegionConnectorMetadata.AT_ZONE_ID
        );
        RequestDataType requestDataType = RequestDataType.METERING_DATA;

        // When
        String result = requestDataType.toString(timeFrame, clock);

        // Then
        assertEquals("MeteringData", result);
    }
}
