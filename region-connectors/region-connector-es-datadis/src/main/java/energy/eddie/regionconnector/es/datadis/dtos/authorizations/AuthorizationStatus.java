// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos.authorizations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public enum AuthorizationStatus {
    PENDING(1, "PENDIENTE"),
    EXPIRED(3, "EXPIRADA"),
    CURRENT(5, "VIGENTE"),
    REJECTED(7, "RECHAZADA"),
    UNKNOWN(-1, "UNKNOWN");

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationStatus.class);
    private final int id;
    private final String description;

    AuthorizationStatus(int id, String description) {
        this.id = id;
        this.description = description;
    }

    @JsonCreator
    public static AuthorizationStatus fromJson(
            @JsonProperty("id") int id,
            @JsonProperty("description") String description
    ) {
        return Arrays.stream(values())
                     .filter(s -> s.id == id)
                     .findFirst()
                     .orElseGet(() -> {
                         LOGGER.warn("Got unknown authorization status with ID: {} and description: {}",
                                     id,
                                     description);
                         return UNKNOWN;
                     });
    }

    @JsonValue
    public Object toJson() {
        return new Object() {
            @JsonProperty("id")
            public final int id = AuthorizationStatus.this.id;
            @JsonProperty("description")
            public final String description = AuthorizationStatus.this.description;
        };
    }
}
