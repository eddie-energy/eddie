// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification._1p13;

import at.ebutilities.schemata.customerprocesses.cpnotification._01p13.CPNotification;
import at.ebutilities.schemata.customerprocesses.cpnotification._01p13.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.cpnotification._01p13.ResponseData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization tests pinning the MapStruct mapping to the values the hand-written
 * {@code EdaCPNotification01p13} wrapper record produced.
 */
class CPNotificationMapperTest {

    @Test
    void testMapping() {
        // Given
        var notification = new CPNotification()
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withConversationId("conv-1")
                                .withResponseData(
                                        new ResponseData()
                                                .withOriginalMessageID("orig-1")
                                                .withResponseCode(7, 8)
                                )
                );

        // When
        var res = CPNotificationMapper.INSTANCE.toEdaCPNotification(notification);

        // Then
        assertEquals("conv-1", res.conversationId());
        assertEquals("orig-1", res.originalMessageId());
        assertEquals(List.of(7, 8), res.responseCodes());
    }
}
