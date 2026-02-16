// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v1_12;

import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.api.v1_12.outbound.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service routes min-max envelope messages between the region connectors. It does that by either using a
 * regionConnectorId, which should match the region connector id or as a fallback uses the country code of the region
 * connector.
 */
@Service
public class MinMaxEnvelopeRouter implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinMaxEnvelopeRouter.class);
    private final Map<String, RegionConnectorMinMaxEnvelopeService> minMaxEnvelopeServices = new HashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    public void registerMinMaxEnvelopeService(
            String regionConnectorId,
            RegionConnectorMinMaxEnvelopeService minMaxEnvelopeService
    ) {
        LOGGER.info("{}: Registering RegionConnectorRetransmissionService: {}",
                    regionConnectorId,
                    minMaxEnvelopeService.getClass().getName());
        minMaxEnvelopeServices.put(regionConnectorId, minMaxEnvelopeService);
    }

    public void registerMinMaxEnvelopeConnector(MinMaxEnvelopeOutboundConnector minMaxEnvelopeOutboundConnector) {
        LOGGER.info("Registering MinMaxEnvelopeOutboundConnector: {}",
                    minMaxEnvelopeOutboundConnector.getClass().getName());
        var subscription = minMaxEnvelopeOutboundConnector.getMinMaxEnvelopes()
                                                          .subscribe(
                                                                  this::route,
                                                                  e -> LOGGER.error("Error in MinMaxEnvelopeRouter", e)
                                                          );
        subscriptions.add(subscription);
    }

    @Override
    public void close() throws Exception {
        for (var subscription : subscriptions) {
            subscription.dispose();
        }
    }

    private void route(RECMMOEEnvelope minMaxEnvelope) {
        var regionConnectorId = getRegionConnectorId(minMaxEnvelope);
        LOGGER.info("Will route MinMaxEnvelope for region connector ID {}", regionConnectorId);

        if (!routeIfServicePresent(regionConnectorId, minMaxEnvelope)) {
            LOGGER.warn("Could not find region connector with id {}", regionConnectorId);
        }
    }

    private static String getRegionConnectorId(RECMMOEEnvelope minMaxEnvelope) {
        var header = minMaxEnvelope.getMessageDocumentHeader();
        var metaInfo = header.getMetaInformation();
        return metaInfo.getRegionConnector();
    }

    private boolean routeIfServicePresent(@Nullable String key, RECMMOEEnvelope minMaxEnvelope) {
        var rc = minMaxEnvelopeServices.get(key);
        if (rc == null) {
            return false;
        }
        rc.minMaxEnvelopeArrived(minMaxEnvelope);
        return true;
    }
}
