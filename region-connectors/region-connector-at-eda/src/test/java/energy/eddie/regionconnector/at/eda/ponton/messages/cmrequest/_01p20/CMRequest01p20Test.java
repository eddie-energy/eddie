package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p20;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

class CMRequest01p20Test {

    @Test
    void toCmRequestWithBlankConfig_throws() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("", null);
        var request = new CMRequest01p20(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         ZonedDateTime.now(AT_ZONE_ID)));

        // when
        // then
        assertThrows(IllegalArgumentException.class, request::cmRequest);
    }

    @Test
    void toCmRequest_withoutPrefixDoesNotAddPrefixToConversationId() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        var request = new CMRequest01p20(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         ZonedDateTime.now(AT_ZONE_ID)));

        // when
        // then
        var res = request.cmRequest();
        assertFalse(res.getProcessDirectory().getConversationId().startsWith("prefix"));
    }

    @Test
    void toCmRequest_withPrefixAddsPrefixToConversationId() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", "prefix-");
        var request = new CMRequest01p20(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         ZonedDateTime.now(AT_ZONE_ID)));

        // when
        // then
        var res = request.cmRequest();
        assertTrue(res.getProcessDirectory().getConversationId().startsWith("prefix-"));
    }
}
