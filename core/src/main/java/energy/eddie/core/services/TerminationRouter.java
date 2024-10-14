package energy.eddie.core.services;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.pmd.MessageTypeList;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.pmd.ReasonCodeTypeList;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This service routes termination messages between the region connectors. It does that by either using a
 * regionConnectorId, which should match the region connector id or as a fallback uses the country code of the region
 * connector.
 */
@Service
public class TerminationRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationRouter.class);
    private final Map<String, RegionConnector> regionConnectors = new HashMap<>();

    public void registerTerminationConnector(TerminationConnector terminationConnector) {
        terminationConnector.getTerminationMessages()
                            .subscribe(this::route,
                                       e -> LOGGER.error("Error in TerminationRouter", e));
    }

    public void registerRegionConnector(RegionConnector regionConnector) {
        LOGGER.info("TerminationRouter: Registering {}", regionConnector.getClass().getName());
        regionConnectors.put(regionConnector.getMetadata().id(), regionConnector);
    }

    private void route(Pair<String, PermissionEnvelope> pmd) {
        if (!isTerminationMessage(pmd)) {
            LOGGER.warn("Received invalid termination message");
            // TODO: Propagate error, related to GH-410
            return;
        }
        String rcId = getRegionConnectorId(pmd);
        String permissionId = pmd.value().getPermissionMarketDocument().getMRID();
        LOGGER.info("Will route PermissionMarketDocument for permissionId {} and region connector ID {}",
                    permissionId,
                    rcId);

        if (!terminateIfRegionConnectorPresent(rcId, permissionId)) {
            LOGGER.warn("Could not find permission request with id {} and region connector id {}", permissionId, rcId);
            // TODO: Propagate error, related to GH-410
        }
    }

    private static boolean isTerminationMessage(Pair<String, PermissionEnvelope> pair) {
        return pair.value()
                   .getPermissionMarketDocument()
                   .getPermissionList()
                   .getPermissions()
                   .getFirst()
                   .getReasonList()
                   .getReasons()
                   .getFirst()
                   .getCode() == ReasonCodeTypeList.CANCELLED_EP
               && pair.value()
                      .getPermissionMarketDocument()
                      .getType() == MessageTypeList.PERMISSION_TERMINATION_DOCUMENT;
    }

    private static String getRegionConnectorId(Pair<String, PermissionEnvelope> pmd) {
        return Optional.ofNullable(pmd.key())
                       .orElseGet(() ->
                                          pmd.value()
                                             .getPermissionMarketDocument()
                                             .getPermissionList()
                                             .getPermissions()
                                             .getFirst()
                                             .getMktActivityRecordList()
                                             .getMktActivityRecords()
                                             .getFirst()
                                             .getType()
                       );
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
