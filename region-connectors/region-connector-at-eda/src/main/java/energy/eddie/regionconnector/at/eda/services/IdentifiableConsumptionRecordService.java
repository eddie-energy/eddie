// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.persistence.JpaPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class IdentifiableConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableConsumptionRecordService.class);
    private final JpaPermissionRequestRepository repository;


    public IdentifiableConsumptionRecordService(
            JpaPermissionRequestRepository repository
    ) {
        this.repository = repository;
    }

    public Optional<IdentifiableConsumptionRecord> mapToIdentifiableConsumptionRecord(EdaConsumptionRecord consumptionRecord) {
        LocalDate startDate = consumptionRecord.startDate();
        LocalDate endDate = consumptionRecord.endDate();
        String meteringPoint = consumptionRecord.meteringPoint();

        // we consider all permission requests that match the metering point and date and are in state SentToPa or after Accepted as relevant.
        // All states after Accepted are relevant, not only because we cant uniquely identify for which PermissionRequest data actually is, but also because
        // DSOs can send data for PermissionRequests that have already been revoked/terminated. This happens for example if some metering values
        // in the Timeframe we still had access are updated (e.g from replacement to actual values).
        // Please note that this causes the ConsumptionRecords to also be emitted for these PermissionRequests (EPs should be able to deal with values for previously received data being updated(
        List<AtPermissionRequestProjection> permissionRequestProjections = repository
                .findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted(
                        meteringPoint,
                        startDate
                );

        List<AtPermissionRequest> permissionRequests = permissionRequestProjections.stream()
                                                                                   .map(EdaPermissionRequest::fromProjection)
                                                                                   .collect(Collectors.toList());

        if (permissionRequests.isEmpty()) {
            LOGGER.warn("No permission requests found for consumption record with date {}", startDate);
            return Optional.empty();
        }

        permissionRequests.forEach(permissionRequest -> LOGGER.info(
                "Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}",
                permissionRequest.conversationId(),
                permissionRequest.permissionId(),
                permissionRequest.connectionId()));

        return Optional.of(new IdentifiableConsumptionRecord(
                consumptionRecord,
                permissionRequests,
                startDate,
                endDate
        ));
    }
}
