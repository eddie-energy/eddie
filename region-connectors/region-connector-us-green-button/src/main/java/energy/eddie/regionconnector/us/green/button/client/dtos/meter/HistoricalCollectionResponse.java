// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class HistoricalCollectionResponse {
    @JsonProperty(required = true)
    private final boolean success;
    @JsonProperty(required = true)
    private final List<String> meters;

    @JsonCreator
    public HistoricalCollectionResponse(boolean success, List<String> meters) {
        this.success = success;
        this.meters = meters;
    }

    public List<String> meters() {
        return meters;
    }

    public boolean isSuccess() {
        return success;
    }
}
