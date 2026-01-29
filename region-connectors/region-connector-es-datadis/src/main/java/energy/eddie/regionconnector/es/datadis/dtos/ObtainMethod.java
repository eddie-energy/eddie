// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.logging.log4j.util.Strings;

public enum ObtainMethod {
    REAL("Real"),
    ESTIMATED("Estimada"),
    UNKNOWN(""); // Represents an empty string or an undefined method

    private final String value;

    ObtainMethod(String value) {
        this.value = value;
    }

    @JsonCreator
    public static ObtainMethod fromString(String text) {
        if (Strings.isBlank(text)) {
            return UNKNOWN;
        }

        for (ObtainMethod method : ObtainMethod.values()) {
            if (method.value.equalsIgnoreCase(text)) {
                return method;
            }
        }
        return UNKNOWN; // Default to UNKNOWN for unmatched or null inputs
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
