// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.converters.AttributeErrorListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import java.util.List;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrMalformedEvent extends PersistablePermissionEvent {
    @Convert(converter = AttributeErrorListConverter.class)
    @Column(name = "errors", columnDefinition = "text")
    private final List<AttributeError> errors;

    protected FrMalformedEvent() {
        errors = List.of();
    }

    public FrMalformedEvent(String permissionId, AttributeError error) {
        this(permissionId, List.of(error));
    }

    public FrMalformedEvent(String permissionId, List<AttributeError> errors) {
        super(permissionId, PermissionProcessStatus.MALFORMED);
        this.errors = errors;
    }
}
