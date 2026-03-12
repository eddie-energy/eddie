// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.agnostic;

import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.api.agnostic.opaque.RegionConnectorOpaqueEnvelopeService;
import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
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
 * This service routes opaque envelopes between the region connectors. It does that by using the regionConnectorId.
 */
@Service
public class OpaqueEnvelopeRouter implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpaqueEnvelopeRouter.class);
    private final Map<String, RegionConnectorOpaqueEnvelopeService> opaqueEnvelopeServices = new HashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    public void registerOpaqueEnvelopeService(
            String regionConnectorId,
            RegionConnectorOpaqueEnvelopeService opaqueEnvelopeService
    ) {
        LOGGER.info("{}: Registering RegionConnectorOpaqueEnvelopeService: {}",
                    regionConnectorId,
                    opaqueEnvelopeService.getClass().getName());
        opaqueEnvelopeServices.put(regionConnectorId, opaqueEnvelopeService);
    }

    public void registerOpaqueEnvelopeConnector(OpaqueEnvelopeOutboundConnector opaqueEnvelopeOutboundConnector) {
        LOGGER.info("Registering OpaqueEnvelopeOutboundConnector: {}",
                    opaqueEnvelopeOutboundConnector.getClass().getName());
        var subscription = opaqueEnvelopeOutboundConnector.getOpaqueEnvelopes()
                                                          .subscribe(
                                                                  this::route,
                                                                  e -> LOGGER.error("Error in OpaqueEnvelopeRouter", e)
                                                          );
        subscriptions.add(subscription);
    }

    @Override
    public void close() throws Exception {
        for (var subscription : subscriptions) {
            subscription.dispose();
        }
    }

    private void route(OpaqueEnvelope opaqueEnvelope) {
        var regionConnectorId = opaqueEnvelope.regionConnectorId();
        LOGGER.info("Will route OpaqueEnvelope for region connector ID {}", regionConnectorId);

        if (!routeIfServicePresent(regionConnectorId, opaqueEnvelope)) {
            LOGGER.warn("Could not find region connector with id {}", regionConnectorId);
        }
    }


    private boolean routeIfServicePresent(@Nullable String key, OpaqueEnvelope opaqueEnvelope) {
        var rc = opaqueEnvelopeServices.get(key);
        if (rc == null) {
            return false;
        }
        rc.opaqueEnvelopeArrived(opaqueEnvelope);
        return true;
    }
}
