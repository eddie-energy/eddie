package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CCMORequestTest {

    @Test
    void constructorWithAllParametersPresent_doesNotThrow() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
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
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");

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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999", "AT9999990699900000000000206868100");
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

    }
}