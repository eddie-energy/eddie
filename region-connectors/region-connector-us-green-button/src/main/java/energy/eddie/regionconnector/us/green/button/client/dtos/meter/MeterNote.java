// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeterNote {
    @JsonProperty(required = true)
    private final String type;
    @JsonProperty(required = true)
    private final ZonedDateTime ts;
    @JsonProperty(required = true)
    private final String msg;

    @JsonCreator
    public MeterNote(String type, ZonedDateTime ts, String msg) {
        this.type = type;
        this.ts = ts;
        this.msg = msg;
    }
}
