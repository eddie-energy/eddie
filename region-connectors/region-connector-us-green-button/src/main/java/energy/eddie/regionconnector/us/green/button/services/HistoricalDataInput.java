// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;

public record HistoricalDataInput(
        IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
