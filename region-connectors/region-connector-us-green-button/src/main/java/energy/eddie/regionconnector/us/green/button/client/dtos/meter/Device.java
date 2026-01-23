// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class Device {
    @JsonProperty(required = true)
    private final String name;
    @JsonProperty(value = "serial_number", required = true)
    private final String serialNumber;

    @JsonCreator
    public Device(String name, String serialNumber) {
        this.name = name;
        this.serialNumber = serialNumber;
    }
}
