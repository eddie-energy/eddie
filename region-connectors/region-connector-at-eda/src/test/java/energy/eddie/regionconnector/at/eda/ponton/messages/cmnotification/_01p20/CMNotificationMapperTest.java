// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p20;

import at.ebutilities.schemata.customerconsent.cmnotification._01p20.CMNotification;
import at.ebutilities.schemata.customerconsent.cmnotification._01p20.ProcessDirectory;
import at.ebutilities.schemata.customerconsent.cmnotification._01p20.ResponseDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization tests pinning the MapStruct mapping to the values the hand-written
 * {@code EdaCMNotification01p20}/{@code ResponseData01p20} wrapper records produced.
 */
class CMNotificationMapperTest {

    @Test
    void testMapping() {
        // Given
        var notification = new CMNotification()
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withConversationId("conv-1")
                                .withCMRequestId("cmreq-1")
                                .withResponseData(
                                        new ResponseDataType()
                                                .withConsentId("c-1")
                                                .withMeteringPoint("AT1111111111111111111111111111111")
                                                .withResponseCode(1, 2),
                                        new ResponseDataType()
                                                .withConsentId("c-2")
                                                .withMeteringPoint("AT2222222222222222222222222222222")
                                                .withResponseCode(3)
                                )
                );

        // When
        var res = CMNotificationMapper.INSTANCE.toEdaCMNotification(notification);

        // Then
        assertEquals("conv-1", res.conversationId());
        assertEquals("cmreq-1", res.cmRequestId());
        assertEquals(2, res.responseData().size());
        assertEquals("c-1", res.responseData().getFirst().consentId());
        assertEquals("AT1111111111111111111111111111111", res.responseData().get(0).meteringPoint());
        assertEquals(List.of(1, 2), res.responseData().get(0).responseCodes());
        assertEquals("c-2", res.responseData().get(1).consentId());
        assertEquals("AT2222222222222222222222222222222", res.responseData().get(1).meteringPoint());
        assertEquals(List.of(3), res.responseData().get(1).responseCodes());
    }
}
