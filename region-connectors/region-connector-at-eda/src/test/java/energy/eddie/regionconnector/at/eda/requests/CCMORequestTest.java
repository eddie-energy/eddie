package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CCMORequestTest {


    @Test
    void messageId_returnsCorrectId() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007");
        ZonedDateTime dt = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        CCMORequest ccmoRequest = new CCMORequest(
                dsoIdAndMeteringPoint,
                timeFrame,
                RequestDataType.METERING_DATA,
                AllowedGranularity.P1D,
                AllowedTransmissionCycle.D,
                atConfiguration,
                dt
        );

        // when
        String messageId = ccmoRequest.messageId();

        // then
        assertEquals("RC100007T946684800000", messageId);
    }

    @Test
    void cmRequestId_returnsCorrectId() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007");
        ZonedDateTime dt = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        CCMORequest ccmoRequest = new CCMORequest(
                dsoIdAndMeteringPoint,
                timeFrame,
                RequestDataType.METERING_DATA,
                AllowedGranularity.P1D,
                AllowedTransmissionCycle.D,
                atConfiguration,
                dt
        );

        // when
        String cmRequestId = ccmoRequest.cmRequestId();

        // then
        assertEquals("KULDH4VX", cmRequestId);
    }
}
