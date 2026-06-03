// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardDirectionTypeList;
import energy.eddie.cim.v1_12.StandardProcessTypeList;
import energy.eddie.cim.v1_12.esr.*;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EdaECMPList;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EnergyCommunityMeteringPointData;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.MeteringPointTimeData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

public class IntermediateEnergySharingReferenceDataMarketDocument {
    private final AtPermissionRequest permissionRequest;
    private final EdaECMPList ecmpList;

    public IntermediateEnergySharingReferenceDataMarketDocument(IdentifiableECMPList id) {
        this.permissionRequest = id.permissionRequest();
        this.ecmpList = id.ecmpList();
    }

    ESRDMDEnvelope toEnvelope() {
        return new ESRDMDEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withCreationDateTime(ZonedDateTime.now(AT_ZONE_ID))
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withRequestPermissionId(permissionRequest.permissionId())
                                                .withConnectionId(permissionRequest.connectionId())
                                                .withDataNeedId(permissionRequest.dataNeedId())
                                                .withRegionConnector(permissionRequest.dataSourceInformation()
                                                                                      .regionConnectorId())
                                                .withRegionCountry(permissionRequest.dataSourceInformation()
                                                                                    .countryCode())
                                                .withDocumentType("energy-sharing-reference-data-market-document")
                                )
                )
                .withMarketDocument(
                        new ESRDMDMarketDocument()
                                .withMRID(ecmpList.messageId())
                                .withRevisionNumber(CommonInformationModelVersions.V1_12.cimify())
                                .withCreatedDateTime(ecmpList.documentCreationDateTime())
                                .withSenderMarketParticipantName(ecmpList.senderMessageAddress())
                                .withReceiverMarketParticipantName(ecmpList.receiverMessageAddress())
                                .withProcessProcessType(StandardProcessTypeList.EXCHANGE_OF_MASTER_DATA.value())
                                .withEnergyCommunities(createEnergyCommunities())
                );
    }

    private List<EnergyCommunity> createEnergyCommunities() {
        List<EnergyCommunity> list = new ArrayList<>();
        for (EnergyCommunityMeteringPointData item : ecmpList.mpListData()) {
            for (MeteringPointTimeData d : item.timeData()) {
                var direction = switch (d.energyDirection()) {
                    case CONSUMPTION -> StandardDirectionTypeList.DOWN.value();
                    case PRODUCTION -> StandardDirectionTypeList.UP.value();
                };
                EnergyCommunity energyCommunity = new EnergyCommunity()
                        .withDateFrom(d.dateFrom())
                        .withMRID(ecmpList.ecId())
                        .withAccountingPoints(
                                new AccountingPoint()
                                        .withEnergySharingParticipationFactor(d.ecPartFact())
                                        .withMRID(new MeasurementPointIDString()
                                                          .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                                          .withValue(item.meteringPoint())
                                        )
                                        .withEnergySharingEnergyDirection(direction)
                        );
                list.add(energyCommunity);
            }
        }
        return list;
    }
}
