package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CCMOTimeFrameTest {

    @Test
    void ccmoTimeFrameValidDates() {
        // given
        ZonedDateTime start = ZonedDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.of(2023, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC);

        // when
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);

        // then
        assertAll(
                () -> assertEquals(start, timeFrame.start()),
                () -> assertTrue(timeFrame.end().isPresent()),
                () -> assertEquals(end, timeFrame.end().get())
        );
    }


    @Test()
    void ccmoTimeFrameNullStart() {
        // given
        ZonedDateTime end = ZonedDateTime.of(2023, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new CCMOTimeFrame(null, end));
    }

    @Test()
    void ccmoTimeFrameInFutureAndEndNull_doesNotThrow() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);

        // when
        // then
        assertDoesNotThrow(() -> new CCMOTimeFrame(start, null));
    }
}