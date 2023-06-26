package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CCMOTimeFrameTest {

    @Test
    void ccmoTimeFrameValidDates() {
        // given
        LocalDate start = LocalDate.of(2023, 5, 1);
        LocalDate end = LocalDate.of(2023, 5, 5);

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
        LocalDate end = LocalDate.of(2023, 5, 5);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new CCMOTimeFrame(null, end));
    }

    @Test()
    void ccmoTimeFrameInPastAndEndNull_throws() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(10);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new CCMOTimeFrame(start, null));
    }

    @Test()
    void ccmoTimeFrameInFutureAndEndNull_doesNotThrow() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(10);

        // when
        // then
        assertDoesNotThrow(() -> new CCMOTimeFrame(start, null));
    }

    @Test()
    void ccmoTimeFrameEndBeforeStart() {
        // given
        LocalDate start = LocalDate.of(2023, 5, 5);
        LocalDate end = LocalDate.of(2023, 5, 1);

        // when
        // then
        assertThrows(IllegalArgumentException.class, () -> new CCMOTimeFrame(start, end));
    }


}