// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PermissionLatestRecordMapTest {
    private static final UUID PERMISSION_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID OTHER_PERMISSION_ID = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");

    @Test
    void lastMessageStream_shouldEmit_whenPutIsCalledAfterSubscription() {
        var map = new PermissionLatestRecordMap();
        var permissionRecord = mock(PermissionLatestRecord.class);

        StepVerifier.create(map.lastMessageStream(PERMISSION_ID))
                    .then(() -> map.put(PERMISSION_ID, permissionRecord))
                    .assertNext(timestamp -> assertTrue(timestamp.isBefore(java.time.Instant.now().plusSeconds(1))))
                    .thenCancel()
                    .verify(Duration.ofSeconds(1));
    }

    @Test
    void lastMessageStream_shouldNotEmit_forADifferentPermission() {
        var map = new PermissionLatestRecordMap();
        var permissionRecord = mock(PermissionLatestRecord.class);

        StepVerifier.create(map.lastMessageStream(PERMISSION_ID))
                    .then(() -> map.put(OTHER_PERMISSION_ID, permissionRecord))
                    .expectNoEvent(Duration.ofMillis(200))
                    .thenCancel()
                    .verify(Duration.ofSeconds(1));
    }

    @Test
    void put_shouldNotThrow_whenNoSubscriberExists() {
        var map = new PermissionLatestRecordMap();
        var permissionRecord = mock(PermissionLatestRecord.class);

        map.put(PERMISSION_ID, permissionRecord);

        assertEquals(permissionRecord, map.get(PERMISSION_ID).orElseThrow());
    }
}
