// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p30;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactoryTest;
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

import static org.junit.jupiter.api.Assertions.*;

class CMRequest01p30OutboundMessageFactoryTest extends CMRequestOutboundMessageFactoryTest {


    @Override
    protected CMRequestOutboundMessageFactory factory() {
        return new CMRequest01p30OutboundMessageFactory(marshaller);
    }

    @Test
    void isActive_on_12_04_2026_returnsFalse() {
        // given
        var factory = new CMRequest01p30OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2026, 4, 12));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_13_04_2026_returnsTrue() {
        // given
        var factory = new CMRequest01p30OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2026, 4, 13));

        // then
        assertTrue(active);
    }

    @Test
    void isMessageType_01p30() {
        // Given
        var factory = new CMRequest01p30OutboundMessageFactory(marshaller);
        var ccmoRequest = new CCMORequest(new DsoIdAndMeteringPoint("dso", null),
                                          new CCMOTimeFrame(LocalDate.now(ZoneOffset.UTC), null),
                                          "cmReqId",
                                          "messageId",
                                          RequestDataType.METERING_DATA,
                                          AllowedGranularity.PT15M,
                                          AllowedTransmissionCycle.D,
                                          new AtConfiguration("ep-id"),
                                          ZonedDateTime.now(ZoneOffset.UTC),
                                          "purpose");

        // When
        var res = factory.outboundMetaData(ccmoRequest);

        // Then
        assertAll(
                () -> assertEquals("CM_REQ_ONL_01.30", res.getMessageType().getSchemaSet().getValue()),
                () -> assertEquals("01.30", res.getMessageType().getVersion().getValue())
        );
    }
}
