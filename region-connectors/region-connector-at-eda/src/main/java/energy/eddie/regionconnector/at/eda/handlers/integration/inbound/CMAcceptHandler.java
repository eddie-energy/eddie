// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.persistence.EdaPermissionEventRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class CMAcceptHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CMAcceptHandler.class);
    private static final Set<PermissionProcessStatus> OPEN_STATUSES = EnumSet.of(
            PermissionProcessStatus.VALIDATED, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
            PermissionProcessStatus.ACCEPTED, PermissionProcessStatus.FULFILLED);
    private final AtPermissionRequestRepository repository;
    private final EdaPermissionEventRepository eventRepository;
    private final Outbox outbox;

    public CMAcceptHandler(
            AtPermissionRequestRepository repository, EdaPermissionEventRepository eventRepository, Outbox outbox
    ) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.outbox = outbox;
    }

    @Transactional
    public void handleCMAccept(CMRequestStatus cmRequestStatus) {
        var consents = validatedConsents(cmRequestStatus.consentData());
        if (consents.isEmpty()) {
            return;
        }
        var requests = matchingRequests(cmRequestStatus);
        if (requests.isEmpty()) {
            LOGGER.warn("No unambiguous permission request for acceptance with conversation ID {} and request ID {}",
                        cmRequestStatus.conversationId(), cmRequestStatus.cmRequestId());
            return;
        }

        var permissionIds = requests.stream().map(AtPermissionRequest::permissionId).toList();
        // The oldest event is shared by all deliveries of this request, including concurrent deliveries on other nodes.
        if (eventRepository.findFirstByPermissionIdInOrderByIdAsc(permissionIds).isEmpty()) {
            throw new IllegalStateException("Permission request has no persisted events");
        }
        // Re-read after acquiring the lock so that a preceding delivery's new permissions are visible.
        requests = matchingRequests(cmRequestStatus);
        if (!eventRepository.findInconsistentPermissionIds(
                requests.stream().map(AtPermissionRequest::permissionId).toList()).isEmpty()) {
            LOGGER.error("Acceptance references inconsistent permission events; reconcile the request before retrying");
            return;
        }
        acceptConsents(consents, requests);
    }

    private List<AtPermissionRequest> matchingRequests(CMRequestStatus status) {
        var candidates = repository.findByOriginalConversationIdOrCMRequestId(status.conversationId(), status.cmRequestId());
        var exact = candidates.stream()
                              .filter(request -> status.conversationId().equals(request.getConversationId()))
                              .toList();
        var matches = exact.isEmpty() ? candidates : exact;
        if (matches.stream().map(CMAcceptHandler::requestContext).distinct().count() != 1) {
            return List.of();
        }
        return matches.stream().<AtPermissionRequest>map(EdaPermissionRequest::fromProjection).toList();
    }

    private static List<Object> requestContext(AtPermissionRequestProjection request) {
        return Arrays.asList(request.getConversationId(), request.getCmRequestId(), request.getConnectionId(),
                             request.getDataNeedId(), request.getDsoId(), request.getPermissionStart(),
                             request.getPermissionEnd(), request.getGranularity(), request.getEnergyDirection(),
                             request.getParticipationFactor());
    }

    private static Collection<ConsentData> validatedConsents(List<ConsentData> consents) {
        Map<String, ConsentData> byConsent = new LinkedHashMap<>();
        Map<String, String> byMeteringPoint = new HashMap<>();
        for (var consent : consents) {
            if (consent.cmConsentId().filter(id -> !id.isBlank()).isEmpty()
                || consent.meteringPoint().filter(id -> !id.isBlank()).isEmpty()) {
                LOGGER.warn("Ignoring acceptance without a consent ID or metering point");
                continue;
            }
            var consentId = consent.cmConsentId().orElseThrow();
            var meteringPoint = consent.meteringPoint().orElseThrow();
            var previous = byConsent.putIfAbsent(consentId, consent);
            var previousConsent = byMeteringPoint.putIfAbsent(meteringPoint, consentId);
            if ((previous != null && !previous.meteringPoint().equals(consent.meteringPoint()))
                || (previousConsent != null && !previousConsent.equals(consentId))) {
                LOGGER.error("Conflicting consent and metering point assignments in acceptance; ignoring message");
                return List.of();
            }
        }
        return byConsent.values();
    }

    private void acceptConsents(Collection<ConsentData> consents, List<AtPermissionRequest> requests) {
        Set<String> usedPermissions = new HashSet<>();
        for (var consent : consents) {
            var consentId = consent.cmConsentId().orElseThrow();
            var meteringPoint = consent.meteringPoint().orElseThrow();
            var assigned = requests.stream().filter(request -> request.consentId().isPresent()
                    && (request.consentId().orElseThrow().equals(consentId)
                        || request.meteringPointId().filter(meteringPoint::equals).isPresent())).toList();
            if (!assigned.isEmpty()) {
                checkAssignment(assigned, consentId, meteringPoint);
                continue;
            }
            var available = requests.stream()
                                    .filter(request -> request.consentId().isEmpty())
                                    .filter(request -> !usedPermissions.contains(request.permissionId()))
                                    .filter(request -> request.status() == PermissionProcessStatus.VALIDATED
                                                       || request.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                                    .toList();
            var matching = available.stream()
                                    .filter(request -> request.meteringPointId().filter(meteringPoint::equals).isPresent())
                                    .toList();
            var targets = matching.isEmpty()
                    ? available.stream().filter(request -> request.meteringPointId().isEmpty()).toList()
                    : matching;
            acceptConsent(consent, requests, targets, usedPermissions);
        }
    }

    private static void checkAssignment(List<AtPermissionRequest> assigned, String consentId, String meteringPoint) {
        if (assigned.size() != 1 || assigned.getFirst().consentId().filter(consentId::equals).isEmpty()
            || assigned.getFirst().meteringPointId().filter(meteringPoint::equals).isEmpty()) {
            LOGGER.error("Consent {} and metering point {} conflict with an existing permission; ignoring acceptance",
                         consentId, meteringPoint);
        }
    }

    private void acceptConsent(
            ConsentData consent, List<AtPermissionRequest> requests, List<AtPermissionRequest> targets,
            Set<String> usedPermissions
    ) {
        if (targets.size() > 1) {
            LOGGER.error("Multiple unassigned permissions match acceptance; reconcile the request before retrying");
            return;
        }
        if (targets.size() == 1) {
            var target = targets.getFirst();
            if (target.status() == PermissionProcessStatus.VALIDATED) {
                sentToPermissionAdministrator(target.permissionId());
            }
            acceptPermissionRequest(consent, target.permissionId());
            usedPermissions.add(target.permissionId());
        } else if (requests.stream().anyMatch(request -> OPEN_STATUSES.contains(request.status()))
                   && requests.stream().noneMatch(request -> request.meteringPointId().equals(consent.meteringPoint()))) {
            createNewPermissionRequest(consent, requests.getFirst());
        }
    }

    private void acceptPermissionRequest(ConsentData consent, String permissionId) {
        outbox.commit(new AcceptedEvent(permissionId, consent.meteringPoint().orElseThrow(),
                                        consent.cmConsentId().orElseThrow(), consent.message()));
    }

    private void sentToPermissionAdministrator(String permissionId) {
        outbox.commit(new EdaAnswerEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                        "Permission administrator has received the request."));
    }

    private void createNewPermissionRequest(ConsentData consent, AtPermissionRequest request) {
        var permissionId = UUID.randomUUID().toString();
        outbox.commit(new CreatedEvent(permissionId, request.connectionId(), request.dataNeedId(), request.created(),
                                       new EdaDataSourceInformation(request.dataSourceInformation().meteredDataAdministratorId()),
                                       consent.meteringPoint().orElseThrow()));
        outbox.commit(new ValidatedEvent(permissionId, request.start(), request.end(), request.granularity(),
                                         request.cmRequestId(), request.conversationId(),
                                         request.energyDirection().orElse(null), request.participationFactor().orElse(null),
                                         ValidatedEvent.NeedsToBeSent.NO));
        sentToPermissionAdministrator(permissionId);
        acceptPermissionRequest(consent, permissionId);
    }
}
