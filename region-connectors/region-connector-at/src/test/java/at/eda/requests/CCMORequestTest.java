package at.eda.requests;

import at.eda.config.AtConfiguration;
import at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CCMORequestTest {

    @Test
    void constructorWithAllParametersPresent_doesNotThrow() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");

        // when
        // then
        assertDoesNotThrow(() ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithDsoIdAndMeteringPointNull_doesNotThrow() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(null, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithTimeFrameNull_throws() {
        // given
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, null, atConfiguration,
                        RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithEddieConfigNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, null,
                        RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithRequestDataTypeNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        null, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D)
        );
    }

    @Test
    void constructorWithMeteringIntervalTypeNull_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");

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
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");

        // when
        // then
        assertThrows(NullPointerException.class, () ->
                new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                        RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, null)
        );
    }

    @Test
    void toCmRequestWithBlankConfig_throws() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("");
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D);

        // when
        // then
        assertThrows(IllegalArgumentException.class, ccmoRequest::toCMRequest);
    }

    @Test
    void toCmRequest() {
        // given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
        ZonedDateTime end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT9999990699900000000000206868100", "AT999999");
        AtConfiguration atConfiguration = new SimpleAtConfiguration("RC100007");
        CCMORequest ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration,
                RequestDataType.METERING_DATA, AllowedMeteringIntervalType.D, AllowedTransmissionCycle.D);

        // when
        // then
        assertDoesNotThrow(ccmoRequest::toCMRequest);
    }

    private record SimpleAtConfiguration(String eligiblePartyId) implements AtConfiguration {
        SimpleAtConfiguration {
            requireNonNull(eligiblePartyId);
        }

        @Override
        public ZoneId timeZone() {
            return ZoneOffset.UTC;
        }
    }
}