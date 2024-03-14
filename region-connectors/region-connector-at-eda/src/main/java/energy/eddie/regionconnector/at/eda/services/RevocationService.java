package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class RevocationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevocationService.class);
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    public RevocationService(EdaAdapter edaAdapter, AtPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
        edaAdapter.getCMRevokeStream()
                .subscribe(this::onRevocation);
    }

    private void onRevocation(CMRevoke cmRevoke) {
        String consentId = cmRevoke.getProcessDirectory().getConsentId();
        var optionalRequest = repository.findByConsentId(consentId);
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
        return repository.findByMeteringPointIdAndDate(cmRevoke.getProcessDirectory().getMeteringPoint(),
                                                       dateTime.toLocalDate());
    }

    private void revoke(AtPermissionRequest permissionRequest) {
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            return;
        }
        outbox.commit(new SimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.REVOKED));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Revoking permission for permission id {}", permissionRequest.permissionId());
        }
    }

}