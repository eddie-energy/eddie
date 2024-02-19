package energy.eddie.core.services;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.cmd.ReasonCodeTypeList;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.adapter.JdkFlowAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This service routes termination messages between the region connectors.
 * It does that by either using a regionConnectorId, which should match the region connector id or as a fallback uses the country code of the region connector.
 */
@Service
public class TerminationRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationRouter.class);
    private final Map<String, RegionConnector> regionConnectors = new HashMap<>();

    public TerminationRouter(Optional<TerminationConnector> terminationConnectorOptional) {
        if (terminationConnectorOptional.isEmpty()) {
            LOGGER.warn("No instance of TerminationConnector found in context, therefore no way that terminations can be received by the core.");
            return;
        }

        var terminationConnector = terminationConnectorOptional.get();
        JdkFlowAdapter.flowPublisherToFlux(terminationConnector.getTerminationMessages())
                .filter(TerminationRouter::isTerminationMessage)
                .log()
                .doOnError(e -> LOGGER.error("Error in TerminationRouter", e))
                .subscribe(this::route);
    }

    private static boolean isTerminationMessage(Pair<String, ConsentMarketDocument> pair) {
        return pair.value().getPermissionList().getPermissions().getFirst().getReasonList().getReasons().getFirst().getCode() == ReasonCodeTypeList.CANCELLED_EP;
    }

    private static String getRegionConnectorId(Pair<String, ConsentMarketDocument> cmd) {
        return Optional.ofNullable(cmd.key())
                .orElseGet(() ->
                        cmd.value()
                                .getPermissionList()
                                .getPermissions()
                                .getFirst()
                                .getMktActivityRecordList()
                                .getMktActivityRecords()
                                .getFirst()
                                .getType()
                );
    }

    public void registerRegionConnector(RegionConnector regionConnector) {
        LOGGER.info("TerminationRouter: Registering {}", regionConnector.getClass().getName());
        regionConnectors.put(regionConnector.getMetadata().id(), regionConnector);
    }

    private void route(Pair<String, ConsentMarketDocument> cmd) {
        String rcId = getRegionConnectorId(cmd);
        String permissionId = cmd.value().getMRID();
        LOGGER.info("Will route ConsentMarketDocument for permissionId {} and region connector ID {}", permissionId, rcId);

        if (!terminateIfRegionConnectorPresent(rcId, permissionId)) {
            LOGGER.warn("Could not find permission request with id {} and region connector id {}", permissionId, rcId);
            // TODO: Propagate error, related to GH-410
        }
    }

    private boolean terminateIfRegionConnectorPresent(@Nullable String key, String permissionId) {
        var rc = regionConnectors.get(key);
        if (rc == null) {
            return false;
        }
        rc.terminatePermission(permissionId);
        return true;
    }
}
