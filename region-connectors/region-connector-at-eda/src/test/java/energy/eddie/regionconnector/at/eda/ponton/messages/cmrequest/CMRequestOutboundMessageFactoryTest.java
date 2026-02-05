// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.MessageId;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MarshallerConfig.class})
@Import(MarshallerConfig.class)
public abstract class CMRequestOutboundMessageFactoryTest {

    @Autowired
    protected Jaxb2Marshaller marshaller;

    @Test
    void createOutboundMessage() {
        // given
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate end = start.plusMonths(1);
        CCMOTimeFrame timeFrame = new CCMOTimeFrame(start, end);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint("AT999999",
                                                                                "AT9999990699900000000000206868100");
        AtConfiguration atConfiguration = new AtConfiguration("RC100007", null);
        ZonedDateTime now = ZonedDateTime.now(AT_ZONE_ID);
        var mesageId = new MessageId(atConfiguration.eligiblePartyId(), now).toString();
        var cmRequestId = new CMRequestId(mesageId).toString();
        var request = new CCMORequest(dsoIdAndMeteringPoint,
                                      timeFrame,
                                      cmRequestId,
                                      mesageId,
                                      AllowedGranularity.P1D,
                                      AllowedTransmissionCycle.D,
                                      atConfiguration,
                                      ZonedDateTime.now(AT_ZONE_ID),
                                      new AccountingPointDataNeed());
        // when
        var message = factory().createOutboundMessage(request);

        // then
        assertNotNull(message);
    }

    protected abstract CMRequestOutboundMessageFactory factory();
}
