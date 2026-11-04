// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.permission.ConnectionStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ConnectionStatusEventStreamTest {
    private final ConnectionStatusEventStream stream = new ConnectionStatusEventStream();

    @Test
    void events_onlyContainEventsOfTheUser() {
        // Given
        var userId = UUID.randomUUID();
        var permissionId = UUID.randomUUID();

        // When, Then
        StepVerifier.create(stream.events(userId))
                    .then(() -> {
                        stream.publish(UUID.randomUUID(), UUID.randomUUID(), ConnectionStatus.CONNECTED);
                        stream.publish(userId, permissionId, ConnectionStatus.CONNECTED);
                    })
                    .expectNext(new ConnectionStatusEventStream.ConnectionStatusEvent(userId,
                                                                                     permissionId,
                                                                                     ConnectionStatus.CONNECTED))
                    .thenCancel()
                    .verify();
    }

    @Test
    void publish_withoutSubscriber_doesNotThrow() {
        assertDoesNotThrow(() -> stream.publish(UUID.randomUUID(), UUID.randomUUID(), ConnectionStatus.NOT_CONNECTED));
    }
}
