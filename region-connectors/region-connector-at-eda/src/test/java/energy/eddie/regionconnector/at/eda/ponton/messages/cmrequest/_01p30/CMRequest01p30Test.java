// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p30;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.EnergyCommunityDataNeed;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.MessageId;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

class CMRequest01p30Test {

    @Test
    void toCmRequestWithBlankConfig_throws() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new AtConfiguration("", null);
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var mesageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(mesageId).toString();
        var request = new CMRequest01p30(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         cmRequestId,
                                                         mesageId,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         now, new AccountingPointDataNeed()));

        // when
        // then
        assertThrows(IllegalArgumentException.class, request::cmRequest);
    }

    @Test
    void toCmRequest_setsSchemaVersion_correctly() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new AtConfiguration("RC100007", null);
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var messageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(messageId).toString();
        var request = new CMRequest01p30(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         cmRequestId,
                                                         messageId,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         now, new AccountingPointDataNeed()));

        // when
        // then
        var res = request.cmRequest();
        assertEquals("01.30", res.getMarketParticipantDirectory().getSchemaVersion());
    }

    @Test
    void toCmRequest_setsPurpose_correctly() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new AtConfiguration("RC100007", null);
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var messageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(messageId).toString();
        var request = new CMRequest01p30(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         cmRequestId,
                                                         messageId,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         now,
                                                         new AccountingPointDataNeed("acc",
                                                                                     "desc",
                                                                                     "purpose",
                                                                                     "https://policy.com",
                                                                                     true,
                                                                                     null)));

        // when
        var res = request.cmRequest();
        // then
        assertEquals("purpose", res.getProcessDirectory().getCMRequest().getPurpose());
    }

    @Test
    void toCmRequest_setsEnergyCommunityAttributes_correctly() {
        // Given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new AtConfiguration("RC100007", "ecid");
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var messageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(messageId).toString();
        var request = new CMRequest01p30(new CCMORequest(dsoIdAndMeteringPoint,
                                                         timeFrame,
                                                         cmRequestId,
                                                         messageId,
                                                         AllowedGranularity.P1D,
                                                         AllowedTransmissionCycle.D,
                                                         atConfiguration,
                                                         now,
                                                         new EnergyCommunityDataNeed(BigDecimal.ONE)));

        // When
        var res = request.cmRequest();

        // Then
        var cmRequest = res.getProcessDirectory().getCMRequest();
        assertAll(
                () -> assertEquals(BigDecimal.ONE, cmRequest.getECPartFact()),
                () -> assertEquals("ecid", cmRequest.getECID())
        );
    }
}
