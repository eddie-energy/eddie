// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger.client.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;
import java.util.List;

public record FindMessages(List<String> adapterIds,
                           @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") ZonedDateTime fromDate,
                           List<Status> inboundStates,
                           List<MessageCategory> messageCategories) {
    public FindMessages(
            String adapterId,
            Status inboundState,
            ZonedDateTime fromDate,
            MessageCategory messageCategory
    ) {
        this(List.of(adapterId), fromDate, List.of(inboundState), List.of(messageCategory));
    }
}
