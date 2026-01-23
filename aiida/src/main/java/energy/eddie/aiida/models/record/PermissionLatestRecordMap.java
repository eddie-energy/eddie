// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PermissionLatestRecordMap {
    private final ConcurrentHashMap<UUID, PermissionLatestRecord>
            permissionRecordMap = new ConcurrentHashMap<>();

    public void put(UUID permissionId, PermissionLatestRecord permissionRecord) {
        permissionRecordMap.put(permissionId, permissionRecord);
    }

    public Optional<PermissionLatestRecord> get(UUID permissionId) {
        return Optional.ofNullable(permissionRecordMap.get(permissionId));
    }
}
