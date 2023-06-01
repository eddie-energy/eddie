package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CCMOTimeFrameTest {

    @Test
    void ccmoTimeFrameValidDates() {
        // given
        ZonedDateTime start = ZonedDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.of(2023, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());

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
        ZonedDateTime end = ZonedDateTime.of(2023, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());

        // when
        // then
        assertThrows(NullPointerException.class, () -> new CCMOTimeFrame(null, end));
    }

    @Test()
    void ccmoTimeFrameInPastAndEndNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new CCMOTimeFrame(start, null));
    }

    @Test()
    void ccmoTimeFrameInFutureAndEndNull_doesNotThrow() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);

        // when
        // then
        assertDoesNotThrow(() -> new CCMOTimeFrame(start, null));
    }

    @Test()
    void ccmoTimeFrameEndBeforeStart() {
        // given
        ZonedDateTime start = ZonedDateTime.of(2023, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault());

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> new CCMOTimeFrame(start, end));
    }


}