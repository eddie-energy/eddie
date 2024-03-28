package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p10;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

class CMRequest01p10Test {

    @Test
    void cmRequest_withBlankConfig_throws() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("", null);
        var request = new CMRequest01p10(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedMeteringIntervalType.D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         ZonedDateTime.now(AT_ZONE_ID)));

        // when
        // then
        assertThrows(IllegalArgumentException.class, request::cmRequest);
    }

    @Test
    void cmRequest_withoutPrefixDoesNotAddPrefixToConversationId() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", null);
        var request = new CMRequest01p10(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedMeteringIntervalType.D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         ZonedDateTime.now(AT_ZONE_ID)));

        // when
        // then
        var res = request.cmRequest();
        assertFalse(res.getProcessDirectory().getConversationId().startsWith("prefix"));
    }

    @Test
    void cmRequest_withPrefixAddsPrefixToConversationId() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007", "prefix-");
        var request = new CMRequest01p10(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedMeteringIntervalType.D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         ZonedDateTime.now(AT_ZONE_ID)));

        // when
        // then
        var res = request.cmRequest();
        assertTrue(res.getProcessDirectory().getConversationId().startsWith("prefix-"));
    }
}
