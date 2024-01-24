package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CCMORequestTest {

    @Test
    void constructorWithAllParametersPresent_doesNotThrow() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);

        // when
        // then
        assertDoesNotThrow(() ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithDsoIdAndMeteringPointNull_doesNotThrow() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(null, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithTimeFrameNull_throws() {
        // given
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, null, atConfiguration,
                        RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithEddieConfigNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, null,
                        RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithRequestDataTypeNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        null, Granularity.P1D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithMeteringIntervalTypeNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, null, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithTransmissionCycleNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, Granularity.P1D, null)
        );
    }

    @Test
    void toCmRequestWithBlankConfig_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("", null);
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D);

        // when
        // then
        assertThrows(IllegalArgumentException.class, ccmoRequest::toCMRequest);
    }

    @Test
    void toCmRequest_withoutPrefixDoesNotAddPrefixToConversationId() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D);

        // when
        // then
        var res = assertDoesNotThrow(ccmoRequest::toCMRequest);
        assertFalse(res.getProcessDirectory().getConversationId().startsWith("prefix"));
    }

    @Test
    void toCmRequest_withPrefixAddsPrefixToConversationId() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", "prefix-");
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D);

        // when
        // then
        var res = assertDoesNotThrow(ccmoRequest::toCMRequest);
        assertTrue(res.getProcessDirectory().getConversationId().startsWith("prefix-"));
    }

    @Test
    void toCmRequest_throwsIfUnsupportedGranularity() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1M, AllowedTransmissionCycle.D);

        // when
        // then
        assertThrows(IllegalArgumentException.class, ccmoRequest::toCMRequest);
    }

    @Test
    void toCmRequest_ifDsoIdDoesNotMatchMeteringPoint_throwsInvalidDsoIdException() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT000000", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D);

        // when
        // then
        assertThrows(InvalidDsoIdException.class, ccmoRequest::toCMRequest);
    }

    @Test
    void messageId_returnsCorrectId() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        ZonedDateTime dt = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D, dt);

        // when
        String messageId = ccmoRequest.messageId();

        // then
        assertEquals("RC100007T946684800000", messageId);
    }

    @Test
    void cmRequestId_returnsCorrectId() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        ZonedDateTime dt = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, Granularity.P1D, AllowedTransmissionCycle.D, dt);

        // when
        String cmRequestId = ccmoRequest.cmRequestId();

        // then
        assertEquals("KULDH4VX", cmRequestId);
    }
}