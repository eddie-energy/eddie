// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public final class HistoricalDataEmissionMetadata {
    private final String namespace;
    private final ZonedDateTime created;

    public HistoricalDataEmissionMetadata() {
        this(UUID.randomUUID().toString(), ZonedDateTime.now(ZoneOffset.UTC));
    }

    private HistoricalDataEmissionMetadata(String namespace, ZonedDateTime created) {
        this.namespace = namespace;
        this.created = created;
    }

    public HistoricalDataEmissionMetadata child(String sourceKey) {
        return new HistoricalDataEmissionMetadata(namespace + "/" + sourceKey.length() + ":" + sourceKey, created);
    }

    public String id(String sourceKey) {
        return UUID.nameUUIDFromBytes((namespace + "/" + sourceKey).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public ZonedDateTime created(ZoneId zone) {
        return created.withZoneSameInstant(zone);
    }

    public ZonedDateTime envelopeCreated() {
        return created;
    }
}
