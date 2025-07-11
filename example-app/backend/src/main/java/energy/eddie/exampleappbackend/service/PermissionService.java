package energy.eddie.exampleappbackend.service;

import energy.eddie.cim.v0_82.pmd.MktActivityRecordComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionMarketDocumentComplexType;
import energy.eddie.exampleappbackend.model.Permission;
import energy.eddie.exampleappbackend.model.PermissionType;
import energy.eddie.exampleappbackend.persistence.PermissionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final DataNeedsService dataNeedsService;
    private final ValidatedHistoricalDataService validatedHistoricalDataService;

    public void handlePermissionEnvelope(PermissionEnvelope permissionEnvelope) {
        var messageDocumentHeaderMetaInformation = permissionEnvelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        var permissionMarketDocument = permissionEnvelope.getPermissionMarketDocument();
        var eddiePermissionId = messageDocumentHeaderMetaInformation.getPermissionid();
        var eddieDataNeedId = messageDocumentHeaderMetaInformation.getDataNeedid();

        var permissionType = dataNeedsService.getPermissionTypeByDataNeedId(eddieDataNeedId);
        if (permissionType == PermissionType.OTHER) {
            log.warn("Data Need for permission is not of type ValidatedHistoricalDataDateNeed or AiidaDataNeed! Permission is ignored!");
            return;
        }

        var existingPermission = permissionRepository.findByEddiePermissionId(eddiePermissionId);
        var status = getLatestStatusForPermission(eddiePermissionId, permissionMarketDocument);
        if (status.isEmpty()) {
            log.warn("Failed to parse Status from Permission Envelope! Ignoring Message!");
            return;
        }

        if (existingPermission.isPresent()) {
            var updatedPermission = existingPermission.get();
            updatedPermission.setStatus(status.get());
            permissionRepository.save(updatedPermission);
            log.info("Updated permission {} with eddie permission id {}. New status is {}.", eddiePermissionId, updatedPermission.getEddiePermissionId(), status.get());
        } else {
            var now = Instant.now();
            String country = switch (permissionType) {
                case VALIDATED_HISTORICAL_DATA -> messageDocumentHeaderMetaInformation.getMessageDocumentHeaderRegion().getCountry().value();
                case REAL_TIME_DATA -> "AIIDA";
                default -> throw new IllegalStateException("Unexpected value: " + permissionType);
            };

            var permission = Permission.builder()
                    .name(generateName(eddiePermissionId, now))
                    .userId(messageDocumentHeaderMetaInformation.getConnectionid())
                    .createdAt(now)
                    .meterResourceId(permissionMarketDocument.getMRID())
                    .status(status.get())
                    .eddieConnectorIdentifier(messageDocumentHeaderMetaInformation.getMessageDocumentHeaderRegion().getConnector())
                    .eddieConnectorCountry(country)
                    .eddiePermissionId(eddiePermissionId)
                    .eddieDataNeedId(eddieDataNeedId)
                    .permissionType(permissionType)
                    .build();

            permissionRepository.save(permission);
            log.info("Created new permission {} with eddie permission id {}.", eddiePermissionId, permission.getEddiePermissionId());

            validatedHistoricalDataService.handleCachedMessageWithEddiePermissionId(eddiePermissionId);
        }
    }

    private Optional<String> getLatestStatusForPermission(String eddiePermissionId, PermissionMarketDocumentComplexType permissionMarketDocument) {
        var permissions = permissionMarketDocument.getPermissionList().getPermissions();
        for (var permission : permissions) {
            if (permission.getPermissionMRID().equals(eddiePermissionId)) {
                var mktActivityRecords = permission
                        .getMktActivityRecordList()
                        .getMktActivityRecords()
                        .stream()
                        .max(Comparator.comparing(MktActivityRecordComplexType::getCreatedDateTime));

                if (mktActivityRecords.isPresent()) {
                    return Optional.of(mktActivityRecords.get().getStatus().value());
                } else {
                    log.warn("Found Permission with missing mktActivityRecords! Element is ignored!");
                }
            } else {
                log.warn("Found Permission in PermissionMarketDocument with mRID, which does not match EDDIE permission id! Element is ignored!");
            }
        }
        return Optional.empty();
    }

    private String generateName(String eddiePermissionId, Instant createdAt) {
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        var formattedCreatedAt = formatter.format(createdAt.atZone(ZoneId.systemDefault()));
        return String.format("Permission %s from %s", eddiePermissionId, formattedCreatedAt);
    }
}
