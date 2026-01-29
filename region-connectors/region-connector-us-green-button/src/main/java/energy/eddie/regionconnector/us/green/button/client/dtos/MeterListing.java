// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.util.List;

public record MeterListing(@JsonProperty(value = "meters", required = true) List<Meter> meters,
                           @Nullable @JsonProperty(value = "next") URI next) {
}
