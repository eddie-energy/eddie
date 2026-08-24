// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PermissionLatestRecordMap {
    private final ConcurrentHashMap<UUID, PermissionLatestRecord>
            permissionRecordMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Sinks.Many<Instant>>
            permissionLastMessageSinks = new ConcurrentHashMap<>();

    public void put(UUID permissionId, PermissionLatestRecord permissionRecord) {
        permissionRecordMap.put(permissionId, permissionRecord);
        sinkFor(permissionId).tryEmitNext(Instant.now());
    }

    public Optional<PermissionLatestRecord> get(UUID permissionId) {
        return Optional.ofNullable(permissionRecordMap.get(permissionId));
    }

    public Flux<Instant> lastMessageStream(UUID permissionId) {
        return sinkFor(permissionId).asFlux();
    }

    private Sinks.Many<Instant> sinkFor(UUID permissionId) {
        return permissionLastMessageSinks.computeIfAbsent(
                permissionId, id -> Sinks.many().multicast().directAllOrNothing());
    }
}
