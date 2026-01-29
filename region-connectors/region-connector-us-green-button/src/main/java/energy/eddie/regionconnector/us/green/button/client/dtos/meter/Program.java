// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class Program {
    @JsonProperty(required = true)
    private final String code;
    @JsonProperty(value = "date_type", required = true)
    private final String dateType;
    @JsonProperty(value = "name", required = true)
    private final String name;

    @JsonCreator
    public Program(String code, String dateType, String name) {
        this.code = code;
        this.dateType = dateType;
        this.name = name;
    }
}
