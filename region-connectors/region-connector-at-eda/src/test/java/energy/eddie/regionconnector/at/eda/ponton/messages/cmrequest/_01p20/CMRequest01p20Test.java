package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p20;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.*;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CMRequest01p20Test {

    @Test
    void toCmRequestWithBlankConfig_throws() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new PlainAtConfiguration("");
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var mesageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(mesageId).toString();
        var request = new CMRequest01p20(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         cmRequestId,
                                                         mesageId,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         now));

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
        AtConfiguration atConfiguration = new PlainAtConfiguration("RC100007");
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var mesageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(mesageId).toString();
        var request = new CMRequest01p20(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         cmRequestId,
                                                         mesageId,
                                                         RequestDataType.METERING_DATA,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         now));

        // when
        // then
        var res = request.cmRequest();
        assertFalse(res.getProcessDirectory().getConversationId().startsWith("prefix"));
    }
}
