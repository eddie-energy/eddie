// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("unused")
public class MeterList {
    @JsonProperty(value = "meters", required = true)
    private final List<String> meterIds;

    @JsonCreator
    public MeterList(List<String> meterIds) {this.meterIds = meterIds;}
}
