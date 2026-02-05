// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p21;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactoryTest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CMRequest01p21OutboundMessageFactoryTest extends CMRequestOutboundMessageFactoryTest {


    @Override
    protected CMRequestOutboundMessageFactory factory() {
        return new CMRequest01p21OutboundMessageFactory(marshaller);
    }

    @Test
    void isActive_on_06_04_2025_returnsFalse() {
        // given
        var factory = factory();

        // when
        var active = factory.isActive(LocalDate.of(2025, 4, 6));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_13_04_2026_returnsFalse() {
        // given
        var factory = factory();

        // when
        var active = factory.isActive(LocalDate.of(2026, 4, 13));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_07_04_2025_returnsTrue() {
        // given
        var factory = factory();

        // when
        var active = factory.isActive(LocalDate.of(2025, 4, 7));

        // then
        assertTrue(active);
    }

    @Test
    void isMessageType_01p21() {
        // Given
        var factory = new CMRequest01p21OutboundMessageFactory(marshaller);
        var ccmoRequest = new CCMORequest(new DsoIdAndMeteringPoint("dso", null),
                                          new CCMOTimeFrame(LocalDate.now(ZoneOffset.UTC), null),
                                          "cmReqId",
                                          "messageId",
                                          AllowedGranularity.PT15M,
                                          AllowedTransmissionCycle.D,
                                          new AtConfiguration("ep-id", null),
                                          ZonedDateTime.now(ZoneOffset.UTC),
                                          new AccountingPointDataNeed());

        // When
        var res = factory.outboundMetaData(ccmoRequest);

        // Then
        assertAll(
                () -> assertEquals("CM_REQ_ONL_01.30", res.getMessageType().getSchemaSet().getValue()),
                () -> assertEquals("01.30", res.getMessageType().getVersion().getValue())
        );
    }
}
