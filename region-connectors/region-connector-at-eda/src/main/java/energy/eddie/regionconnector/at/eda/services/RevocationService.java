package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class RevocationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevocationService.class);
    private final PermissionRequestService permissionRequestService;

    public RevocationService(EdaAdapter edaAdapter, PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
        edaAdapter.getCMRevokeStream()
                .subscribe(this::onRevocation);
    }

    private void onRevocation(CMRevoke cmRevoke) {
        String consentId = cmRevoke.getProcessDirectory().getConsentId();
        var optionalRequest = permissionRequestService.findByConsentId(consentId);
        if (optionalRequest.isPresent()) {
            revoke(optionalRequest.get());
            return;
        }
        LOGGER.warn("Got CM Revoke Message with unknown consent id: {}; Using fallback and querying permission request by metering point id and revoke date.", consentId);
        var requests = fallback(cmRevoke);
        if (requests.isEmpty()) {
            LOGGER.error("Got Revoke Message with unknown consent id: {}; Could not revoke.", consentId);
        } else {
            // Revoke every permission since we do not know which permission request was actually revoked
            requests.forEach(this::revoke);
        }
    }

    private List<AtPermissionRequest> fallback(CMRevoke cmRevoke) {
        ZonedDateTime dateTime = XmlGregorianCalenderUtils.toUtcZonedDateTime(cmRevoke.getProcessDirectory().getConsentEnd());
        return permissionRequestService.findByMeteringPointIdAndDate(cmRevoke.getProcessDirectory().getMeteringPoint(), dateTime.toLocalDate());
    }

    private void revoke(AtPermissionRequest permissionRequest) {
        try {
            permissionRequest.revoke();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Revoking permission for permission id {}", permissionRequest.permissionId());
            }
        } catch (StateTransitionException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Could not revoke permission with permission id: {}", permissionRequest.permissionId(), e);
            }
        }
    }

}
