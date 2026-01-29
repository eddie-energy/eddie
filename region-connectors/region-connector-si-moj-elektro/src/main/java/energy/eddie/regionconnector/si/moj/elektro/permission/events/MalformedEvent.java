// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro.permission.events;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.converters.AttributeErrorListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import java.util.List;

@Entity(name = "SiMalformedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class MalformedEvent extends PersistablePermissionEvent {
    @Convert(converter = AttributeErrorListConverter.class)
    @Column(name = "errors", columnDefinition = "text")
    private List<AttributeError> attributeErrors;

    public MalformedEvent(String permissionId, List<AttributeError> attributeErrors) {
        super(permissionId, PermissionProcessStatus.MALFORMED);
        this.attributeErrors = attributeErrors;
    }

    public MalformedEvent(String permissionId, AttributeError attributeErrors) {
        this(permissionId, List.of(attributeErrors));
    }

    protected MalformedEvent() { }
}
